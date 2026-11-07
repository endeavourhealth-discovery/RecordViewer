import Keycloak from "keycloak-js";
import ApiPopulation from "@/API/ApiPopulation";

const keycloakInstance = new Keycloak();

interface CallbackOneParam<T1 = void, T2 = void> {
    (param1: T1): T2;
}

const initKeycloak = () => {
    keycloakInstance.init({ onLoad: "login-required", checkLoginIframe: false });
}

const Login = (onAuthenticatedCallback: CallbackOneParam) => {
    keycloakInstance
        .init({ onLoad: "login-required", checkLoginIframe: false })
        .then(async function (authenticated) {
            if (authenticated) {
                await ApiPopulation.getUserProjectID();
                beginRefreshCycle();
                onAuthenticatedCallback();
            } else {
                alert("non authenticated");
            }
        })
        .catch(() => {
            // keycloak init failed
        });
};

const UserName = (): string | undefined =>
    keycloakInstance?.tokenParsed?.preferred_username;
const Token = (): string | undefined => keycloakInstance?.token;
const LogOut = () => {
    sessionStorage.clear()
    keycloakInstance.logout()
};
const UserID = (): string | undefined => keycloakInstance.tokenParsed?.sub;
const confirmUserRole = (role: string) => {
    return keycloakInstance.hasRealmRole(role);
}

function beginRefreshCycle() {
    setTimeout(refreshToken, 59000);
}
function isUserPatient() {
    return confirmUserRole("rv_patient")
}
async function refreshToken() {

    await keycloakInstance.updateToken(59).then(() => {
        beginRefreshCycle();
    }).catch(() => { });

}

const KeycloakService = {
    CallLogin: Login,
    GetUserName: UserName,
    GetUserID: UserID,
    GetAccessToken: Token,
    CallLogOut: LogOut,
    init: initKeycloak,
    ConfirmUserRole: confirmUserRole,
    IsUserPatient: isUserPatient,
};
export default KeycloakService;
