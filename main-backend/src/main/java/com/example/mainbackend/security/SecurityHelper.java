package com.example.mainbackend.security;

import com.example.mainbackend.entity.User;
import com.example.mainbackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityHelper {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final VacationRepository vacationRepository;
    private final SettlementRepository settlementRepository;
    private final TaskConstraintRepository taskConstraintRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal()))
            throw new IllegalStateException("No authenticated user found");

        String nationalId;
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails)
            nationalId = ((UserDetails) principal).getUsername();
        else if (principal instanceof String)
            nationalId = (String) principal;
        else
            throw new IllegalStateException("Unknown principal type: " + principal.getClass());

        return userRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new IllegalStateException("User not found in database: " + nationalId));
    }

    public boolean hasRole(String roleName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return false;

        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + roleName));
    }

    public boolean isAdmin() { return hasRole("ADMIN"); }

    public boolean isManager() { return hasRole("MANAGER"); }

    public boolean isWorker() { return hasRole("WORKER"); }

    public Long getCurrentUserDepartmentId() {
        if (isAdmin()) return null; // Global access

        User user = getCurrentUser();
        if (user.getDepartment() == null)
            throw new IllegalStateException("Manager user has no department assigned");
        return user.getDepartment().getId();
    }

    // ABAC (Attribute-Based Access Control) Methods for @PreAuthorize

    // Checks if the current user is allowed to access/manage the given department.
    //  ADMINs can manage any department. MANAGERs can only manage their own department.
    public boolean canManageDepartment(Long targetDepartmentId) {
        if (targetDepartmentId == null) return false;
        if (isAdmin()) return true;
        if (isManager())
            return targetDepartmentId.equals(getCurrentUserDepartmentId());
        return false; // Workers cannot manage departments
    }

    // Checks if the current user is operating on their own data.
    public boolean isSelf(Long targetUserId) {
        if (targetUserId == null) return false;
        User currentUser = getCurrentUser();
        return currentUser.getId().equals(targetUserId);
    }

    // Checks if the current user is allowed to access a specific user's sensitive data.
    // Allowed if: It's the user themselves, OR their manager, OR an Admin.
    public boolean canManageUser(Long targetUserId) {
        if (targetUserId == null) return false;
        if (isAdmin()) return true;
        if (isSelf(targetUserId)) return true;

        if (isManager()) {
            // Need to fetch the target user to check if they belong to the manager's department
            User targetUser = userRepository.findById(targetUserId).orElse(null);
            if (targetUser == null || targetUser.getDepartment() == null) return false;
            return targetUser.getDepartment().getId().equals(getCurrentUserDepartmentId());
        }
        return false;
    }

    // TasksetStatus-based Access Control Methods for @PreAuthorize

    // Checks if the user can MANAGE (edit/delete) a specific task.
    // Admin: Yes. Manager: Only if task is in their department. Worker: No.
    public boolean canManageTask(Long taskId) {
        if (taskId == null) return false;
        if (isAdmin()) return true;
        if (isManager())
            return taskRepository.findById(taskId)
                    .map(task -> task.getDepartment().getId().equals(getCurrentUserDepartmentId()))
                    .orElse(false);

        return false;
    }

    // Checks if the user can VIEW a specific task.
    // Admin/Manager: Same as manage. Worker: Only if assigned to them.
    public boolean canViewTask(Long taskId) {
        if (canManageTask(taskId)) return true; // If you can manage it, you can view it
        if (isWorker()) {
            Long currentUserId =getCurrentUser().getId();
            return settlementRepository.existsByTask_IdAndUser_Id(taskId, currentUserId);
        }
        return false;
    }

    // Vacation Access Control Methods for @PreAuthorize

    // Checks if the user can MANAGE (approve/reject/edit) a specific vacation.
    // Admin: Yes. Manager: Only if the worker belongs to their department. Worker: No.
    public boolean canManageVacation(Long vacationId) {
        if (vacationId == null) return false;
        if (isAdmin()) return true;

        if (isManager())
            return vacationRepository.findById(vacationId)
                    .map(vacation -> vacation.getUser().getDepartment().getId().equals(getCurrentUserDepartmentId()))
                    .orElse(false);

        return false;
    }

    // Checks if the user can VIEW a specific vacation.
    // Admin/Manager: Same as manage. Worker: Only if it's their own vacation.
    public boolean canViewVacation(Long vacationId) {
        if (canManageVacation(vacationId)) return true;

        // If it's a regular worker, check if it's their own vacation
        return vacationRepository.findById(vacationId)
                .map(vacation -> vacation.getUser().getId().equals(getCurrentUser().getId()))
                .orElse(false);
    }

    // Settlement Access Control Methods for @PreAuthorize

    // Checks if the user can MANAGE (delete/edit) a specific settlement.
    // Admin: Yes. Manager: Only if the settlement belongs to their department.
    public boolean canManageSettlement(Long settlementId) {
        if (settlementId == null) return false;
        if (isAdmin()) return true;

        if (isManager())
            return settlementRepository.findById(settlementId)
                    .map(settlement -> settlement.getTask().getDepartment().getId().equals(getCurrentUserDepartmentId()))
                    .orElse(false);

        return false; // Workers cannot manage/delete settlements
    }

    // Checks if the user can VIEW a specific settlement.
    // Admin/Manager: Same as manage. Worker: Only if it's assigned to them.
    public boolean canViewSettlement(Long settlementId) {
        if (canManageSettlement(settlementId)) return true;

        if (isWorker())
            return settlementRepository.findById(settlementId)
                    .map(settlement -> settlement.getUser().getId().equals(getCurrentUser().getId()))
                    .orElse(false);

        return false;
    }

    // Constraint Access Control Methods for @PreAuthorize

    public boolean canManageConstraint(Long constraintId) {
        if (constraintId == null) return false;
        if (isAdmin()) return true;
        if (isManager())
            return taskConstraintRepository.findById(constraintId)
                    .map(constraint -> constraint.getPredecessorTask().getDepartment().getId().equals(getCurrentUserDepartmentId()))
                    .orElse(false);

        return false;
    }

    public boolean canViewConstraint(Long constraintId) {
        if (canManageConstraint(constraintId)) return true;
        if (isWorker())
            return taskConstraintRepository.findById(constraintId)
                    .map(constraint -> canViewTask(constraint.getPredecessorTask().getId()) ||
                            canViewTask(constraint.getSuccessorTask().getId()))
                    .orElse(false);

        return false;
    }
}

