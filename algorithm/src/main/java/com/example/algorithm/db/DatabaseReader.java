package com.example.algorithm.db;

import com.example.algorithm.model.AlgoTask;
import com.example.algorithm.model.AlgoUser;
import com.example.algorithm.model.AlgoVacation;

import java.sql.*;
import java.util.*;

/**
 * Reads all scheduling-relevant data from the database.
 * Uses plain JDBC — no Spring, no ORM overhead.
 *
 * Call {@link #loadAll()} to get a fully populated {@link ScheduleData}
 * snapshot ready for the algorithm to consume.
 */
public class DatabaseReader {

    // -------------------------------------------------------------------------
    // Public entry point
    // -------------------------------------------------------------------------

    /**
     * Opens a connection, reads users, vacations and tasks, then returns
     * a self-contained {@link ScheduleData} object.
     */
    public ScheduleData loadAll() {
        try (Connection conn = openConnection()) {
            List<AlgoUser>  users  = loadUsers(conn);
            List<AlgoTask>  tasks  = loadTasks(conn, users);
            return new ScheduleData(users, tasks);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load schedule data from DB", e);
        }
    }

    // -------------------------------------------------------------------------
    // Users
    // -------------------------------------------------------------------------

    private List<AlgoUser> loadUsers(Connection conn) throws SQLException {
        Map<Long, AlgoUser> userMap = new LinkedHashMap<>();

        // 1. Base user data
        String userSql = """
                SELECT id, first_name, last_name, email,
                       daily_availability_hours, max_tasks
                FROM user
                """;
        try (PreparedStatement ps = conn.prepareStatement(userSql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                AlgoUser u = AlgoUser.builder()
                        .id(rs.getLong("id"))
                        .firstName(rs.getString("first_name"))
                        .lastName(rs.getString("last_name"))
                        .email(rs.getString("email"))
                        .dailyAvailabilityHours(rs.getObject("daily_availability_hours", Integer.class))
                        .maxTasks(rs.getObject("max_tasks", Integer.class))
                        .roles(new HashSet<>())
                        .vacations(new ArrayList<>())
                        .build();
                userMap.put(u.getId(), u);
            }
        }

        // 2. User roles
        String roleSql = """
                SELECT ur.user_id, r.role_name
                FROM user_role ur
                JOIN role r ON r.id = ur.role_id
                """;
        try (PreparedStatement ps = conn.prepareStatement(roleSql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long userId = rs.getLong("user_id");
                if (userMap.containsKey(userId)) {
                    userMap.get(userId).getRoles().add(rs.getString("role_name"));
                }
            }
        }

        // 3. Approved vacations
        String vacSql = """
                SELECT v.id, v.user_id, v.start_date, v.end_date, vs.name AS status
                FROM vacation v
                JOIN vacation_status vs ON vs.id = v.vacation_status_id
                WHERE vs.name = 'APPROVED'
                """;
        try (PreparedStatement ps = conn.prepareStatement(vacSql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long userId = rs.getLong("user_id");
                if (!userMap.containsKey(userId)) continue;

                AlgoVacation vac = AlgoVacation.builder()
                        .id(rs.getLong("id"))
                        .userId(userId)
                        .startDate(rs.getDate("start_date").toLocalDate())
                        .endDate(rs.getDate("end_date").toLocalDate())
                        .status(rs.getString("status"))
                        .build();
                userMap.get(userId).getVacations().add(vac);
            }
        }

        return new ArrayList<>(userMap.values());
    }

    // -------------------------------------------------------------------------
    // Tasks
    // -------------------------------------------------------------------------

    private List<AlgoTask> loadTasks(Connection conn, List<AlgoUser> users) throws SQLException {
        // Build a quick lookup map so we can link assigned employees
        Map<Long, AlgoUser> userMap = new HashMap<>();
        for (AlgoUser u : users) userMap.put(u.getId(), u);

        Map<Long, AlgoTask> taskMap = new LinkedHashMap<>();

        // 1. Base task data
        String taskSql = """
                SELECT t.id, t.title, t.description, t.duration_hours,
                       t.deadline, t.start_time,
                       p.name AS priority_name, p.value AS priority_level,
                       ts.name AS status_name,
                       t.user_id AS assigned_user_id
                FROM task t
                JOIN priority p  ON p.id  = t.priority_id
                JOIN task_status ts ON ts.id = t.task_status_id
                """;
        try (PreparedStatement ps = conn.prepareStatement(taskSql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Long assignedUserId = rs.getObject("assigned_user_id", Long.class);

                AlgoTask task = AlgoTask.builder()
                        .id(rs.getLong("id"))
                        .title(rs.getString("title"))
                        .description(rs.getString("description"))
                        .durationHours(rs.getObject("duration_hours", Integer.class))
                        .deadline(toLocalDateTime(rs.getTimestamp("deadline")))
                        .startTime(toLocalDateTime(rs.getTimestamp("start_time")))
                        .priority(rs.getString("priority_name"))
                        .priorityLevel(rs.getObject("priority_level", Integer.class))
                        .status(rs.getString("status_name"))
                        .assignedEmployee(assignedUserId != null ? userMap.get(assignedUserId) : null)
                        .requiredRoles(new HashSet<>())
                        .predecessorTaskIds(new ArrayList<>())
                        .successorTaskIds(new ArrayList<>())
                        .build();
                taskMap.put(task.getId(), task);
            }
        }

        // 2. Required roles per task
        String roleSql = """
                SELECT trr.task_id, r.role_name
                FROM task_required_roles trr
                JOIN role r ON r.id = trr.role_id
                """;
        try (PreparedStatement ps = conn.prepareStatement(roleSql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long taskId = rs.getLong("task_id");
                if (taskMap.containsKey(taskId)) {
                    taskMap.get(taskId).getRequiredRoles().add(rs.getString("role_name"));
                }
            }
        }

        // 3. Task constraints (predecessor → successor)
        String constraintSql = """
                SELECT tc.predecessor_task_id, tc.successor_task_id
                FROM task_constraint tc
                """;
        try (PreparedStatement ps = conn.prepareStatement(constraintSql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long predId = rs.getLong("predecessor_task_id");
                long succId = rs.getLong("successor_task_id");
                if (taskMap.containsKey(predId)) {
                    taskMap.get(predId).getSuccessorTaskIds().add(succId);
                }
                if (taskMap.containsKey(succId)) {
                    taskMap.get(succId).getPredecessorTaskIds().add(predId);
                }
            }
        }

        return new ArrayList<>(taskMap.values());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
                DbConfig.getUrl(),
                DbConfig.getUsername(),
                DbConfig.getPassword()
        );
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}


