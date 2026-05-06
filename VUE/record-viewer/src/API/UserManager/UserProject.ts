import {ApplicationPolicyAttribute} from "@/API/UserManager/ApplicationPolicyAttribute";

export class UserProject {
    id!: string;
    userId!: string;
    projectId! :string;
    projectName!: string;
    organisationId!: string;
    organisationName!: string;
    deleted!: boolean;
    default!: boolean;
    applicationPolicyAttributes!: ApplicationPolicyAttribute[];
}
