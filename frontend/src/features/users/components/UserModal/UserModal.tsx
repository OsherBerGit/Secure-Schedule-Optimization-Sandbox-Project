import { useState, useEffect, type FormEvent, type ChangeEvent } from "react";
import { departmentApi, skillApi } from "../../../../api";
import type { User, Department, Skill, UserAvailability, CreateUserRequest, UpdateUserRequest } from "../../../../types";
import { X, User as UserIcon } from "lucide-react";
import UserAvailabilityEditor from "../UserAvailabilityEditor";
import "./UserModal.css";

interface UserModalProps {
    user: User | null;
    departments: Department[];
    skills: Skill[];
    onSubmit: (data: CreateUserRequest | UpdateUserRequest) => Promise<void> | void;
    onClose: () => void;
}

interface UserFormData {
    nationalId: string;
    password: string;
    firstName: string;
    lastName: string;
    email: string;
    phoneNumber: string;
    role: "ADMIN" | "MANAGER" | "WORKER";
    departmentName: string;
    maxTasks: number | "";
    skillIds: number[];
    availabilities: UserAvailability[];
}

const checkOverlappingShifts = (availRows: UserAvailability[]): string | null => {
    for (let i = 0; i < availRows.length; i++) {
        for (let j = i + 1; j < availRows.length; j++) {
            const r1 = availRows[i];
            const r2 = availRows[j];
            if (r1.dayOfWeek === r2.dayOfWeek && r1.startTime < r2.endTime && r2.startTime < r1.endTime)
                return `Overlapping shifts detected on ${r1.dayOfWeek}`;
        }
    }
    return null;
};

const UserModal = ({ user, onSubmit, onClose }: UserModalProps) => {
    const [formData, setFormData] = useState<UserFormData>({
        nationalId: user?.nationalId ?? "",
        password: "",
        firstName: user?.firstName ?? "",
        lastName: user?.lastName ?? "",
        email: user?.email ?? "",
        phoneNumber: user?.phoneNumber ?? "",
        role: user?.role ?? "WORKER",
        departmentName: user?.departmentName ?? "",
        maxTasks: user?.maxTasks ?? 5,
        skillIds: user?.skills?.map((s: Skill) => s.id) ?? [],
        availabilities: user?.availabilities ? [...user.availabilities] : []
    });

    const [departments, setDepartments] = useState<Department[]>([]);
    const [skills, setSkills] = useState<Skill[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [errorMsg, setErrorMsg] = useState<string | null>(null);
    const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
    const [isSubmitting, setIsSubmitting] = useState(false);

    useEffect(() => {
        let isMounted = true;
        setIsLoading(true);
        Promise.all([departmentApi.getAll(), skillApi.getAll()])
            .then(([deptRes, skillRes]) => {
                if (isMounted) {
                    setDepartments(deptRes.data);
                    setSkills(skillRes.data);
                }
            })
            .catch(() => {})
            .finally(() => {
                if (isMounted) setIsLoading(false);
            });
        return () => {
            isMounted = false;
        };
    }, []);

    const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value, type } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: type === "number" ? (value === "" ? "" : parseInt(value, 10)) : value
        }));
    };

    const handleSkillChange = (skillId: number) => {
        setFormData(prev => ({
            ...prev,
            skillIds: prev.skillIds.includes(skillId) ? prev.skillIds.filter(id => id !== skillId) : [...prev.skillIds, skillId]
        }));
    };

    const handleAddAvail = () => {
        setFormData(prev => ({
            ...prev,
            availabilities: [
                ...prev.availabilities,
                {
                    id: null,
                    dayOfWeek: "SUNDAY",
                    startTime: "08:00:00",
                    endTime: "17:00:00"
                }
            ]
        }));
    };

    const handleRemoveAvail = (index: number) => {
        setFormData(prev => ({
            ...prev,
            availabilities: prev.availabilities.filter((_, i) => i !== index)
        }));
    };

    const handleChangeAvail = (index: number, field: keyof UserAvailability, value: string) => {
        setFormData(prev => {
            const updated = [...prev.availabilities];
            updated[index] = { ...updated[index], [field]: value };
            return { ...prev, availabilities: updated };
        });
    };

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setErrorMsg(null);
        setFieldErrors({});
        setIsSubmitting(true);

        const overlapError = checkOverlappingShifts(formData.availabilities);
        if (overlapError) {
            setErrorMsg(overlapError);
            setIsSubmitting(false);
            return;
        }

        try {
            const baseData = {
                firstName: formData.firstName || undefined,
                lastName: formData.lastName || undefined,
                email: formData.email || undefined,
                phoneNumber: formData.phoneNumber || undefined,
                role: formData.role,
                departmentName: formData.departmentName || null,
                maxTasks: formData.maxTasks || 0,
                availabilities: formData.availabilities,
                skillIds: formData.skillIds
            };

            const submitData = user
                ? baseData
                : {
                      ...baseData,
                      nationalId: formData.nationalId,
                      password: formData.password,
                      departmentName: formData.departmentName || undefined
                  };

            await onSubmit(submitData as CreateUserRequest | UpdateUserRequest);
        } catch (err: unknown) {
            const response = err?.response;
            if (response?.status === 400 && typeof response.data === "object") {
                const errs: Record<string, string> = {};
                for (const [f, m] of Object.entries(response.data)) if (typeof m === "string") errs[f] = m;

                if (Object.keys(errs).length > 0) {
                    setFieldErrors(errs);
                    return;
                }
                if (response.data.message) {
                    setErrorMsg(response.data.message);
                    return;
                }
            }

            const errorMessage = err instanceof Error ? err.message : "Request failed.";
            setErrorMsg(response?.data?.message || errorMessage);
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="modal-overlay">
            <div className="user-modal-card responsive-modal">
                <div className="modal-header">
                    <h2>
                        <UserIcon size={22} className="text-primary" /> {user ? "Edit User" : "Add New User"}
                    </h2>
                    <button type="button" className="modern-close-btn" onClick={onClose}>
                        <X size={24} />
                    </button>
                </div>
                <form onSubmit={handleSubmit} className="user-modal-form">
                    <div className="user-modal-body padded-body">
                        {errorMsg && <div className="error-message banner-spacing">{errorMsg}</div>}
                        <div className="form-grid default-gap">
                            <div className="modern-form-group">
                                <label className="form-label">National ID</label>
                                <input
                                    name="nationalId"
                                    className="modern-input large-input"
                                    value={formData.nationalId}
                                    onChange={handleChange}
                                    required
                                    placeholder="e.g. 123456789"
                                    disabled={!!user}
                                />
                                {fieldErrors.nationalId && <small className="field-error-text">{fieldErrors.nationalId}</small>}
                            </div>
                            <div className="modern-form-group">
                                <label className="form-label">Password</label>
                                <input
                                    name="password"
                                    className="modern-input large-input"
                                    type="password"
                                    value={formData.password}
                                    onChange={handleChange}
                                    required={!user}
                                    minLength={6}
                                    placeholder={user ? "Leave blank to keep current" : "Min 6 characters"}
                                />
                                {fieldErrors.password && <small className="field-error-text">{fieldErrors.password}</small>}
                            </div>
                            <div className="modern-form-group">
                                <label className="form-label">First Name</label>
                                <input
                                    name="firstName"
                                    type="text"
                                    className="modern-input large-input"
                                    placeholder="Enter first name"
                                    value={formData.firstName}
                                    onChange={handleChange}
                                    required
                                />
                            </div>
                            <div className="modern-form-group">
                                <label className="form-label">Last Name</label>
                                <input
                                    name="lastName"
                                    type="text"
                                    className="modern-input large-input"
                                    placeholder="Enter last name"
                                    value={formData.lastName}
                                    onChange={handleChange}
                                    required
                                />
                            </div>
                            <div className="modern-form-group">
                                <label className="form-label">Email Address</label>
                                <input
                                    name="email"
                                    type="email"
                                    className="modern-input large-input"
                                    placeholder="Enter secure email"
                                    value={formData.email}
                                    onChange={handleChange}
                                    required
                                />
                            </div>
                            <div className="modern-form-group">
                                <label className="form-label">Phone Number</label>
                                <input
                                    name="phoneNumber"
                                    type="tel"
                                    className="modern-input large-input"
                                    placeholder="Enter phone number"
                                    value={formData.phoneNumber}
                                    onChange={handleChange}
                                />
                            </div>
                            <div className="modern-form-group">
                                <label className="form-label">Access Role</label>
                                <select name="role" className="modern-input large-input" value={formData.role} onChange={handleChange}>
                                    <option value="WORKER">WORKER</option>
                                    <option value="MANAGER">MANAGER</option>
                                    <option value="ADMIN">ADMIN</option>
                                </select>
                                {fieldErrors.role && <small className="field-error-text">{fieldErrors.role}</small>}
                            </div>
                            <div className="modern-form-group">
                                <label className="form-label">Assigned Department</label>
                                <select name="departmentName" className="modern-input large-input" value={formData.departmentName} onChange={handleChange}>
                                    <option value="">- None -</option>
                                    {departments.map(dept => (
                                        <option key={dept.id} value={dept.name}>
                                            {dept.name}
                                        </option>
                                    ))}
                                </select>
                                {fieldErrors.departmentName && <small className="field-error-text">{fieldErrors.departmentName}</small>}
                            </div>
                            <div className="modern-form-group">
                                <label className="form-label">Max Tasks per Week</label>
                                <input
                                    name="maxTasks"
                                    className="modern-input large-input"
                                    type="number"
                                    placeholder="e.g. 5"
                                    value={formData.maxTasks}
                                    onChange={handleChange}
                                    required
                                    min={1}
                                />
                                {fieldErrors.maxTasks && <small className="field-error-text">{fieldErrors.maxTasks}</small>}
                            </div>

                            {/* Personnel Skills Section */}
                            <div className="modern-form-group full-width-span top-margin">
                                <span className="modal-section-title">Personnel Skills</span>
                                <div className="skills-container-wrapper">
                                    {isLoading ? (
                                        <p>Loading skills...</p>
                                    ) : (
                                        <div className="skills-selection-grid">
                                            {skills.map(skill => (
                                                <label key={skill.id} className="skill-checkbox-item">
                                                    <input
                                                        type="checkbox"
                                                        checked={formData.skillIds.includes(skill.id)}
                                                        onChange={() => handleSkillChange(skill.id)}
                                                    />
                                                    <span>{skill.name}</span>
                                                </label>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            </div>

                            {/* User Availability Section */}
                            <div className="modern-form-group full-width-span top-margin">
                                <div className="availability-header">
                                    <span className="modal-section-title" style={{ marginBottom: 0 }}>
                                        User Availability
                                    </span>
                                    <button type="button" className="btn-add-shift" onClick={handleAddAvail} disabled={isSubmitting}>
                                        + Add Shift
                                    </button>
                                </div>
                                <div className="availability-container-wrapper">
                                    <UserAvailabilityEditor
                                        availabilities={formData.availabilities}
                                        onAdd={handleAddAvail}
                                        onRemove={handleRemoveAvail}
                                        onChange={handleChangeAvail}
                                        disabled={isSubmitting}
                                        hideHeader={true}
                                    />
                                </div>
                            </div>
                        </div>
                    </div>
                    <div className="modal-actions modal-actions-footer">
                        <button type="button" className="btn-cancel" onClick={onClose} disabled={isSubmitting}>
                            Cancel
                        </button>
                        <button type="submit" className="btn-submit" disabled={isSubmitting}>
                            {isSubmitting ? "Saving..." : "Save User"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default UserModal;
