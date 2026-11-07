import { UserProject } from "@/API/UserManager/UserProject";
import { UserProfile } from "@/API/UserManager/UserProfile";
import KeycloakService from "@/KeycloakService";
import axios from "@/axiosConfig";
import apiPopulation from "@/API/ApiPopulation";
const clientId = 'record-viewer'
const API_URL = "userManager/";
let selectedProject: any = null;
let userProfilePromise: Promise<UserProfile> | null = null;
let userProjectsPromise: Promise<UserProject[]> | null = null;

async function loadUserProfile(): Promise<UserProfile> {
    const response = await axios.get(`${API_URL}getUserProfile`, {
        method: "GET",
        headers: {
            Accept: "*/*",
            Authorization: "bearer " + KeycloakService.GetAccessToken(),
        },
        params: { userId: KeycloakService.GetUserID() },
    });
    return response.data;
}

async function loadUserProjects(): Promise<UserProject[]> {
    const response = await axios.get(`${API_URL}getProjects`, {
        method: "GET",
        headers: {
        Accept: "*/*",
            Authorization: "bearer " + KeycloakService.GetAccessToken(),
    },
        params: { userId: KeycloakService.GetUserID() },
    });
    return response.data;
}

async function getUserProfile(force = false): Promise<UserProfile> {
    if (!userProfilePromise || force) {
        userProfilePromise = loadUserProfile();
    }
    return userProfilePromise;
}

async function getUserProjects(force = false): Promise<UserProject[]> {
    if (!userProjectsPromise || force) {
        userProjectsPromise = loadUserProjects();
    }
    return userProjectsPromise;
}

async function getSelectedProject(): Promise<UserProject> {
    if (selectedProject) return selectedProject;
    const projects = await getUserProjects();
    const def = projects.find((p) => p.default) || projects[0];

    selectedProject = def;
    apiPopulation.setActiveProject(selectedProject);
    return def;
}

function setSelectedProject(project: any){
    selectedProject = project
    apiPopulation.setActiveProject(selectedProject);
}

async function changeDefaultProject(defaultProject: string, userProjectId: string) {
    const params = {
        userId: KeycloakService.GetUserID(),
        defaultProjectId: defaultProject,
        userProjectId: userProjectId,
    };
    await axios.get(`${API_URL}setDefaultProject`, {
        method: "GET",
            headers: {
                Accept: "*/*",
                Authorization: "Bearer " + KeycloakService.GetAccessToken(),
        },
        params: params,
        responseType: "text",
    });
}

async function checkRoleAccess(role: string): Promise<boolean> {
    if (!role) return true;
    const application = clientId;
    const up = await getUserProfile();
    const selected = await getSelectedProject();
    const org = up.organisationProjects.find(
        (x) => x.organisation.uuid === selected.organisationId
    );
    if (!org) return false;
    const attributes =
        org.projects.find((y) => y.uuid === selected.projectId)
            ?.applicationPolicyAttributes || [];
    const appAttributes = attributes.filter((x) => x.application === application);
    return appAttributes.some(
        (x) => x.applicationAccessProfileName === role
    );
}

async function getCurrentAccessLevels(){
    const application = clientId;
    const up = await getUserProfile();
    const selected = await getSelectedProject();
    const org = up.organisationProjects.find(
        (x) => x.organisation.uuid === selected.organisationId
    );
    if (!org) return false;
    const attributes =
        org.projects.find((y) => y.uuid === selected.projectId)
            ?.applicationPolicyAttributes || [];
    const appAttributes = attributes.filter((x) => x.application === application);
    return appAttributes
}

async function checkRolesAccessForProject(roles: string[], project: UserProject): Promise<boolean> {
    if (!roles) return true;
    const application = clientId;
    const up = await getUserProfile();
    const selected = project;
    const org = up.organisationProjects.find(
        (x) => x.organisation.uuid === selected.organisationId
    );
    if (!org) return false;

    const attributes =
        org.projects.find((y) => y.uuid === selected.projectId)
            ?.applicationPolicyAttributes || [];
    const appAttributes = attributes.filter((x) => x.application === application);

    for(let i = 0; i < roles.length; i++){
        if(appAttributes.some(
            (x) => x.applicationAccessProfileName === roles[i]
        )) {
            return true;
        }
    }
    return false;
}

async function getUserName(){
    const up = await getUserProfile();
    return up.username;
}

const UserManager = {
    GetUserProfile:getUserProfile,
    GetUserProjects:getUserProjects,
    GetUserName: getUserName,
    GetSelectedProject:getSelectedProject,
    ChangeDefaultProject:changeDefaultProject,
    CheckRoleAccess:checkRoleAccess,
    SetSelectedProject: setSelectedProject,
    checkRolesAccessForProject,
    getCurrentAccessLevels
};

export default UserManager;
