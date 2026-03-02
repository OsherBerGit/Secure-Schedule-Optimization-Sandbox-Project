import { useState } from 'react'
import type { FormEvent } from 'react'
import type { User, CreateUserRequest, UpdateUserRequest } from '../types'

interface UserModalProps {
    user: User | null
    onSubmit: (data: CreateUserRequest | UpdateUserRequest) => void
    onClose: () => void
}

const UserModal = ({ user, onSubmit, onClose }: UserModalProps) => {
    const [nationalId, setNationalId] = useState(user?.nationalId ?? '')
    const [password, setPassword] = useState('')
    const [firstName, setFirstName] = useState(user?.firstName ?? '')
    const [lastName, setLastName] = useState(user?.lastName ?? '')
    const [email, setEmail] = useState(user?.email ?? '')
    const [phoneNumber, setPhoneNumber] = useState(user?.phoneNumber ?? '')
    const derivedRole: 'ADMIN' | 'WORKER' = user?.role ?? (user?.roles?.includes('ADMIN') ? 'ADMIN' : 'WORKER')
    const [role, setRole] = useState<'ADMIN' | 'WORKER'>(derivedRole)

    function handleSubmit(e: FormEvent) {
        e.preventDefault()
        if (user) {
            const data: UpdateUserRequest = { firstName: firstName || undefined, lastName: lastName || undefined, email: email || undefined, phoneNumber: phoneNumber || undefined, role }
            onSubmit(data)
        } else {
            const data: CreateUserRequest = { nationalId, password, firstName, lastName, email, phoneNumber, role }
            onSubmit(data)
        }
    }

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal" onClick={e => e.stopPropagation()}>

                <div className="modal-header">
                    <h2>{user ? 'Edit User' : 'Add User'}</h2>
                    <button className="btn-close" onClick={onClose}>✕</button>
                </div>

                <form onSubmit={handleSubmit} className="modal-form">

                    {/* Only show nationalId + password when creating */}
                    {!user && (
                        <>
                            <div className="form-group">
                                <label>National ID</label>
                                <input value={nationalId} onChange={e => setNationalId(e.target.value)} required placeholder="e.g. 123456789" />
                            </div>
                            <div className="form-group">
                                <label>Password</label>
                                <input type="password" value={password} onChange={e => setPassword(e.target.value)} required />
                            </div>
                        </>
                    )}

                    <div className="form-group">
                        <label>First Name</label>
                        <input value={firstName ?? ''} onChange={e => setFirstName(e.target.value)} />
                    </div>

                    <div className="form-group">
                        <label>Last Name</label>
                        <input value={lastName ?? ''} onChange={e => setLastName(e.target.value)} />
                    </div>

                    <div className="form-group">
                        <label>Email</label>
                        <input type="email" value={email ?? ''} onChange={e => setEmail(e.target.value)} />
                    </div>

                    <div className="form-group">
                        <label>Phone Number</label>
                        <input value={phoneNumber ?? ''} onChange={e => setPhoneNumber(e.target.value)} />
                    </div>

                    <div className="form-group">
                        <label>Role</label>
                        <select value={role} onChange={e => setRole(e.target.value as 'ADMIN' | 'WORKER')}>
                            <option value="WORKER">WORKER</option>
                            <option value="ADMIN">ADMIN</option>
                        </select>
                    </div>

                    <div className="modal-footer">
                        <button type="button" className="btn-cancel" onClick={onClose}>Cancel</button>
                        <button type="submit" className="btn-save">Save</button>
                    </div>

                </form>
            </div>
        </div>
    )
}

export default UserModal
