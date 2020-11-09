import { Role } from './role';

export class Account {
    id: string;
    firstName: string;
    lastName: string;
    username: string;
    email: string;
    password: string;
    role: Role;
    hasNewsletter: boolean;
    jwtToken?: string;
}