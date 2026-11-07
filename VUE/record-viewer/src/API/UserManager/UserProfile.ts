import {Region} from "@/API/UserManager/Region";
import {UserOrganisationProject} from "@/API/UserManager/UserOrganisationProject";

export class UserProfile {
    uuid!: string;
    username!: string;
    forename!: string;
    surname!: string;
    email!: string;
    mobile!: string;
    photo!: string;
    region!: Region;
    organisationProjects!: UserOrganisationProject[];
}
