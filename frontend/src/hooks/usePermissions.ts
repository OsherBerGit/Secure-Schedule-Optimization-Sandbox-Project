import { useAuth } from "../context/useAuth";

export const usePermissions = () => {
    const { user } = useAuth();

    const isAdmin = user?.role === "ADMIN";
    const isManager = user?.role === "MANAGER";

    return {
        isAdmin,
        isManager,
        isWorker: user?.role === "WORKER",

        canAdd: isAdmin || isManager,
        canDelete: isAdmin,
        canEdit: isAdmin || isManager,
        canViewReports: isAdmin,
    };
};