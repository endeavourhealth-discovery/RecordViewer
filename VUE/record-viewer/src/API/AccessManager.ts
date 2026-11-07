import UserManager from '@/API/UserManager/UserManager'
import { RouteLocationNormalized } from 'vue-router';

let _url = "";
let userAccessLevel = "Viewer";

async function GetCurrentAccessLevel(){
    const currentAccessLevels = await UserManager.getCurrentAccessLevels();
    userAccessLevel = "Viewer"
    if(currentAccessLevels) {
        if (currentAccessLevels.find(x => x.applicationAccessProfileName === 'record-viewer'))
            userAccessLevel = "record-viewer"
    }
    return userAccessLevel;
}

async function isAccessAllowed(roles: string[], route: RouteLocationNormalized): Promise<boolean> {
    let path = "";
    if(route.path.includes("&"))
        path = route.path.substring(0, route.path.indexOf("&"))
    else path = route.path;
    if (!path.endsWith("unauthorised")) {
        _url = route.fullPath; // save attempted route
    }

    for(let i = 0; i < roles.length; i++){
        if(await CheckRoleAccess(roles[i])){
            return true;
        }
    }
    return false;
}

async function CheckRoleAccess(role: string): Promise<boolean> {
    try {
        const authorised = await UserManager.CheckRoleAccess(role);
        return authorised;
    } catch (error) {
        return Promise.reject(error);
    }
}

function getUrl(){
    return _url;
}
function getCleanUrl(){
    if(_url.includes("&"))
        return _url.substring(0, _url.indexOf("&"))
    else return _url;
}

const AccessControl = {
    isAccessAllowed,
    CheckRoleAccess,
    getUrl,
    getCleanUrl,
    GetCurrentAccessLevel
};

export default AccessControl;
