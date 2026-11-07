import KeycloakService from "@/KeycloakService";
import { UserProject } from "@/API/UserManager/UserProject";
import UserManager from "@/API/UserManager/UserManager";

const API_URL = "/record-viewer/api/events/VUE/";
let userProjectID = "";
let projectID = "";

const EventType = {
    CONDITIONS: "1",
    OBSERVATIONS: "2",
    PROCEDURES: "3",
    FAMILY_HISTORY: "4",
    IMMUNISATIONS: "5",
    PROCEDURE_REQUESTS: "6",
    DIAGNOSTIC_ORDERS: "7",
    WARNINGS: "8",
} as const;

let activeProject: UserProject;

async function fetchResults(url: string, request: RequestInit) {
    const response = await fetch(url, request);
    if (!response.ok) {
        throw new Error(`API request failed: ${response.status} ${response.statusText}`);
    }
    return await response.json();
}

function buildPatientSearchURL(name: string, nhsNumber: string, dob: string) {
    const searchURL = new URL(window.location.protocol + window.location.host + API_URL + "patients");
    searchURL.searchParams.set("page", "0");
    searchURL.searchParams.set("size", "10");
    searchURL.searchParams.set("name", name);
    searchURL.searchParams.append("nhsNumber", nhsNumber);
    searchURL.searchParams.set("dob", dob);
    return searchURL
}

function buildSinglePatientSearchURL(id: string) {
    const searchURL = new URL(window.location.protocol + window.location.host + API_URL + "patient");
    searchURL.searchParams.set("id", id);
    return searchURL
}

function buildSearchRequest(endPoint: string, personID: string) {
    const searchUrl = new URL(window.location.protocol + window.location.host + API_URL + endPoint);
    searchUrl.searchParams.append("personID", personID);
    return searchUrl
}

function buildAdditionalSearchRequest(endPoint: string, resourceType: string, resourceId: string) {
    const searchUrl = new URL(window.location.protocol + window.location.host + API_URL + endPoint);
    searchUrl.searchParams.append("type", resourceType);
    searchUrl.searchParams.append("ID", resourceId);
    return searchUrl
}

function buildGETRequest() {
    return {
        method: "GET",
        headers: {
            Accept: "*/*",
            "Accept-Encoding": "gzip, deflate, br",
            Connection: "keep-alive",
            Authorization: "bearer " + KeycloakService.GetAccessToken(),
            "projectID": projectID,
        },
    };
}

async function getUserProjectID() {
    if (!activeProject) {
        setActiveProject(await UserManager.GetSelectedProject());
    }
    return userProjectID;
}


async function populateMedication(personID: string, activeOnly: string) {
    const request = buildGETRequest();
    const searchURL = buildSearchRequest("medication", personID);
    searchURL.searchParams.append("active", activeOnly);
    const meds = await fetchResults(searchURL.toString(), request);
    return meds["results"];
}

async function searchForSinglePatient(nhsNumber: string) {
    const request = buildGETRequest();
    const searchURL = buildPatientSearchURL("", nhsNumber, "");
    const data = await fetchResults(searchURL.toString(), request);
    return data["results"][0];
}

async function searchForSinglePatientWithId(patientId: string) {
    const searchURL = buildSinglePatientSearchURL(patientId);
    const request = buildGETRequest();
    const pat = await fetchResults(searchURL.toString(), request);
    return pat["results"];
}

async function searchForPatients(name: string, nhsNumber: string, dob: string, page: string) {
    const searchUrl = buildPatientSearchURL(name, nhsNumber, dob);
    const request = buildGETRequest();
    const pats = await fetchResults(searchUrl.toString(), request);
    return pats["results"];
}

async function populateAllergies(personID: string) {
    const searchURL = buildSearchRequest("allergy", personID);
    const request = buildGETRequest();
    const allergies = await fetchResults(searchURL.toString(), request);
    return allergies["results"];
}

async function populateDiagnostics(personID: string, summary: string) {
    const searchUrl = buildSearchRequest("diagnostics", personID);
    const request = buildGETRequest();
    searchUrl.searchParams.set("term", "");
    searchUrl.searchParams.set("summaryMode", summary);
    const diagnoses = await fetchResults(searchUrl.toString(), request);
    return diagnoses["results"];
}

async function populateDiagnosticOrders(personID: string) {
    const response = await populateObservations(personID, "1", "0", EventType.DIAGNOSTIC_ORDERS);
    const orders = await response.json();
    return orders["results"];
}

async function populateAppointments(personID: string) {
    const searchUrl = buildSearchRequest("appointment", personID);
    const request = buildGETRequest();
    const data = await fetchResults(searchUrl.toString(), request);
    return data["results"];
}

async function populateAdditionals(type: string, id: string) {
    const searchUrl = buildAdditionalSearchRequest("additional", type, id);
    const request = buildGETRequest();
    const data = await fetchResults(searchUrl.toString(), request);
    return data["results"];
}

async function getConceptNameFromId(id: string) {
    const searchUrl = buildSearchRequest("conceptName", id);
    const request = buildGETRequest();
    return await fetchResults(searchUrl.toString(), request);
}

async function populateSummaryObservations(personID: string) {
    const response = await populateObservations(personID, "1", "0", EventType.OBSERVATIONS);
    const observations = await response.json();
    return observations["results"];
}

async function populateProcedures(personID: string, summaryMode: string) {
    const response = await populateObservations(personID, summaryMode, "0", EventType.PROCEDURES);
    const procedures = await response.json();
    return procedures["results"];
}

async function populateAllObservations(personID: string) {
    const response = await populateObservations(personID, "0", "0", EventType.OBSERVATIONS);
    const observations = await response.json();
    return observations["results"];
}

async function populateObservations(personID: string, summary: string, activeOnly: string, eventType: string) {
    const searchUrl = buildSearchRequest("observation", personID);
    searchUrl.searchParams.append("eventType", eventType);
    searchUrl.searchParams.append("active", activeOnly);
    searchUrl.searchParams.append("term", "");
    searchUrl.searchParams.append("summaryMode", summary)

    const request = buildGETRequest();
    return await fetch(searchUrl.toString(), request);
}

async function populateReferralRequests(personID: string) {
    const searchUrl = buildSearchRequest("referrals", personID);
    const request = buildGETRequest();
    const referrals = await fetchResults(searchUrl.toString(), request);
    return referrals["results"];
}

async function populateActiveConditionsProblems(personID: string) {
    const response = await populateObservations(personID, "0", "1", EventType.CONDITIONS);
    const activeIssues = await response.json();
    return activeIssues["results"];
}

async function populateConditionsProblems(personID: string) {
    const response = await populateObservations(personID, "0", "0", EventType.CONDITIONS);
    const issues = await response.json();
    return issues["results"];
}

async function populateWarningsAndFlags(personID: string) {
    const response = await populateObservations(personID, "0", "0", EventType.WARNINGS);
    const warnings = await response.json();
    return warnings["results"];
}

async function populateProcedureRequests(personID: string) {
    const response = await populateObservations(personID, "0", "0", EventType.PROCEDURE_REQUESTS);
    const requests = await response.json();
    return requests["results"];
}

async function populateFamilyHistory(personID: string) {
    const response = await populateObservations(personID, "0", "0", EventType.FAMILY_HISTORY);
    const requests = await response.json();
    return requests["results"];
}

async function populateImmunisations(personID: string, activeOnly: string) {
    const response = await populateObservations(personID, "0", activeOnly, EventType.IMMUNISATIONS);
    const immunisations = await response.json();
    return immunisations["results"];
}

async function populateEncounters(personID: string, summary: string) {
    const searchUrl = buildSearchRequest("encounters", personID);
    searchUrl.searchParams.append("summaryMode", summary);

    const request = buildGETRequest();
    const encounters = await fetchResults(searchUrl.toString(), request);
    return encounters["results"];
}

async function populateEncounterTree(encounterId: string) {
    const searchUrl = new URL(window.location.protocol + window.location.host + API_URL + "encounterTreeRoot");
    searchUrl.searchParams.append("encounterID", encounterId);
    const request = buildGETRequest();
    const tree = await fetchResults(searchUrl.toString(), request);
    return tree["results"];
}

async function populateEncounterTreeMedication(encounterId: string) {
    const searchUrl = new URL(window.location.protocol + window.location.host + API_URL + "encounterTreeMedication");
    searchUrl.searchParams.append("encounterID", encounterId);
    const request = buildGETRequest();
    const tree = await fetchResults(searchUrl.toString(), request);
    return tree["results"];
}

async function populateEncounterTreeObservation(encounterId: string) {
    const searchUrl = new URL(window.location.protocol + window.location.host + API_URL + "encounterTreeObservation");
    searchUrl.searchParams.append("encounterID", encounterId);
    const request = buildGETRequest();
    const tree = await fetchResults(searchUrl.toString(), request);
    return tree["results"];
}



function setActiveProject(selected: UserProject) {
    activeProject = selected;
    userProjectID = selected.id;
    projectID = selected.projectId;
}

const ApiPopulation = {
    nhsNumber: "",
    populateMedication: populateMedication,
    searchForPatient: searchForSinglePatient,
    searchForPatients: searchForPatients,
    searchForSinglePatientWithId: searchForSinglePatientWithId,
    populateAllergies: populateAllergies,
    populateDiagnostics: populateDiagnostics,
    populateDiagnosticOrders: populateDiagnosticOrders,
    populateFamilyHistory: populateFamilyHistory,
    populateAppointments: populateAppointments,
    populateAdditionals: populateAdditionals,
    getConceptNameFromId: getConceptNameFromId,
    populateProcedureRequests: populateProcedureRequests,
    populateConditionsProblems: populateConditionsProblems,
    populateProcedures: populateProcedures,
    populateEncounterTreeObservation: populateEncounterTreeObservation,
    populateActiveObservations: populateSummaryObservations,
    populateAllObservations: populateAllObservations,
    populateWarningsAndFlags: populateWarningsAndFlags,
    populateActiveConditionsProblems: populateActiveConditionsProblems,
    populateEncounters: populateEncounters,
    populateEncounterTree: populateEncounterTree,
    populateEncounterTreeMedication: populateEncounterTreeMedication,
    populateImmunisations: populateImmunisations,
    populateReferralRequests: populateReferralRequests,
    getUserProjectID: getUserProjectID,
    setActiveProject: setActiveProject,
    generateAiReview: generateAiReview,
    sendAiChat: sendAiChat,
    sendAiChatWithAttachment: sendAiChatWithAttachment,
    clearAiCache: clearAiCache,
    adminAnalyze: adminAnalyze,
    adminSearchPatient: adminSearchPatient,
    generateGpSummary: generateGpSummary,
    sendGpChat: sendGpChat,
    sendGpChatWithAttachment: sendGpChatWithAttachment,
    sendGpChatMode: sendGpChatMode,
    sendGpChatModeWithAttachment: sendGpChatModeWithAttachment,
    clearGpCache: clearGpCache,
}

export interface ChatMessage {
    role: 'user' | 'assistant';
    content: string;
    images?: string[];  // base64 data URLs for vision support
    hasAttachment?: boolean;
    attachmentType?: 'image' | 'document';
    prompt_tokens?: number;
    completion_tokens?: number;
    total_tokens?: number;
    max_tokens?: number;
    max_context?: number;
    thinking_content?: string;
}

export interface EmergencyFlags {
    has_active_allergies: boolean;
    has_warnings: boolean;
    has_drug_interactions: boolean;
    critical_findings: string[];
}

export interface AiReviewResponse {
    summary: string;
    emergency_flags: EmergencyFlags;
    recommendations: string[];
    chat_response?: string;
    prompt_tokens?: number;
    completion_tokens?: number;
    total_tokens?: number;
    max_tokens?: number;
    max_context?: number;
    thinking_content?: string;
}

async function generateAiReview(personID: string, symptoms?: string, purpose?: string): Promise<AiReviewResponse> {
    await getUserProjectID();

    const body: any = { personID };
    if (symptoms) body.symptoms = symptoms;
    if (purpose) body.purpose = purpose;

    const request = {
        method: "POST",
        headers: {
            Accept: "*/*",
            Authorization: "bearer " + KeycloakService.GetAccessToken(),
            "projectID": projectID,
            "Content-Type": "application/json",
        },
        body: JSON.stringify(body),
    };
    const searchURL = new URL(window.location.protocol + window.location.host + API_URL + "ai-review/generate-summary");
    const data = await fetchResults(searchURL.toString(), request as unknown as RequestInit);
    return data;
}

async function sendAiChat(personID: string, conversationHistory: ChatMessage[], symptoms?: string, purpose?: string): Promise<AiReviewResponse> {
    await getUserProjectID();

    const body: any = { personID, conversationHistory };
    if (symptoms) body.symptoms = symptoms;
    if (purpose) body.purpose = purpose;

    const request = {
        method: "POST",
        headers: {
            Accept: "*/*",
            Authorization: "bearer " + KeycloakService.GetAccessToken(),
            "projectID": projectID,
            "Content-Type": "application/json",
        },
        body: JSON.stringify(body),
    };
    const searchURL = new URL(window.location.protocol + window.location.host + API_URL + "ai-review/chat");
    const data = await fetchResults(searchURL.toString(), request as unknown as RequestInit);
    return data;
}

async function sendAiChatWithAttachment(personID: string, conversationHistory: ChatMessage[], symptoms: string | undefined, attachmentFile: File | null, purpose?: string): Promise<AiReviewResponse> {
    await getUserProjectID();

    const formData = new FormData();
    formData.append("personID", personID);
    formData.append("conversationHistory", JSON.stringify(conversationHistory));
    if (symptoms) {
        formData.append("symptoms", symptoms);
    }
    if (purpose) {
        formData.append("purpose", purpose);
    }

    if (attachmentFile) {
        // Append attachment file - ensure it's a real File/Blob, not a Vue proxy
        const rawFile = attachmentFile instanceof File ? attachmentFile : new File([attachmentFile as Blob], (attachmentFile as any).name || "attachment", { type: (attachmentFile as any).type || "application/octet-stream" });
        formData.append("images", rawFile, rawFile.name);
    }

    // Use relative URL to avoid any host/protocol duplication issues
    const url = API_URL + "ai-review/chat-with-images";
    const request = {
        method: "POST",
        headers: {
            Authorization: "bearer " + KeycloakService.GetAccessToken(),
            "projectID": projectID,
        },
        body: formData,
    };
    const data = await fetchResults(url, request as unknown as RequestInit);
    return data;
}

async function clearAiCache(personID?: string): Promise<{status: string}> {
    let url = window.location.protocol + window.location.host + API_URL + "ai-review/cache/clear";
    if (personID) {
        const searchURL = new URL(url);
        searchURL.searchParams.set("personID", personID);
        searchURL.searchParams.set("projectID", projectID || "");
        url = searchURL.toString();
    }
    const request = buildGETRequest();
    return await fetchResults(url, request);
}

async function adminAnalyze(correspondenceText: string, knownPatientName?: string, knownPatientDob?: string, knownNhsNumber?: string, attachment?: File | null): Promise<AiReviewResponse> {
    await getUserProjectID();

    if (attachment) {
        const formData = new FormData();
        formData.append("correspondenceText", correspondenceText || "");
        if (knownPatientName) formData.append("knownPatientName", knownPatientName);
        if (knownPatientDob) formData.append("knownPatientDob", knownPatientDob);
        if (knownNhsNumber) formData.append("knownNhsNumber", knownNhsNumber);
        formData.append("attachment", attachment, attachment.name);

        const url = API_URL + "ai-review/admin-analyze-upload";
        const request = {
            method: "POST",
            headers: {
                Authorization: "bearer " + KeycloakService.GetAccessToken(),
                "projectID": projectID,
            },
            body: formData,
        };
        const data = await fetchResults(url, request as unknown as RequestInit);
        return data;
    }

    const body: any = { correspondenceText };
    if (knownPatientName) body.knownPatientName = knownPatientName;
    if (knownPatientDob) body.knownPatientDob = knownPatientDob;
    if (knownNhsNumber) body.knownNhsNumber = knownNhsNumber;

    const request = {
        method: "POST",
        headers: {
            Accept: "*/*",
            Authorization: "bearer " + KeycloakService.GetAccessToken(),
            "projectID": projectID,
            "Content-Type": "application/json",
        },
        body: JSON.stringify(body),
    };
    const searchURL = new URL(window.location.protocol + window.location.host + API_URL + "ai-review/admin-analyze");
    const data = await fetchResults(searchURL.toString(), request as unknown as RequestInit);
    return data;
}

async function adminSearchPatient(name: string, nhsNumber?: string, dob?: string): Promise<any> {
    const searchURL = new URL(window.location.protocol + window.location.host + API_URL + "ai-review/admin-search-patient");
    if (name) searchURL.searchParams.set("name", name);
    if (nhsNumber) searchURL.searchParams.set("nhsNumber", nhsNumber);
    if (dob) searchURL.searchParams.set("dob", dob);

    const request = buildGETRequest();
    return await fetchResults(searchURL.toString(), request);
}

async function generateGpSummary(personID: string, symptoms?: string): Promise<AiReviewResponse> {
    await getUserProjectID();

    const body: any = { personID };
    if (symptoms) body.symptoms = symptoms;

    const request = {
        method: "POST",
        headers: {
            Accept: "*/*",
            Authorization: "bearer " + KeycloakService.GetAccessToken(),
            "projectID": projectID,
            "Content-Type": "application/json",
        },
        body: JSON.stringify(body),
    };
    const searchURL = new URL(window.location.protocol + window.location.host + API_URL + "ai-review/gp-summary");
    const data = await fetchResults(searchURL.toString(), request as unknown as RequestInit);
    return data;
}

async function sendGpChat(personID: string, conversationHistory: ChatMessage[], symptoms?: string): Promise<AiReviewResponse> {
    await getUserProjectID();

    const body: any = { personID, conversationHistory };
    if (symptoms) body.symptoms = symptoms;

    const request = {
        method: "POST",
        headers: {
            Accept: "*/*",
            Authorization: "bearer " + KeycloakService.GetAccessToken(),
            "projectID": projectID,
            "Content-Type": "application/json",
        },
        body: JSON.stringify(body),
    };
    const searchURL = new URL(window.location.protocol + window.location.host + API_URL + "ai-review/gp-chat");
    const data = await fetchResults(searchURL.toString(), request as unknown as RequestInit);
    return data;
}

async function sendGpChatWithAttachment(personID: string, conversationHistory: ChatMessage[], symptoms: string | undefined, attachmentFile: File | null): Promise<AiReviewResponse> {
    await getUserProjectID();

    const formData = new FormData();
    formData.append("personID", personID);
    formData.append("conversationHistory", JSON.stringify(conversationHistory));
    if (symptoms) {
        formData.append("symptoms", symptoms);
    }

    if (attachmentFile) {
        const rawFile = attachmentFile instanceof File ? attachmentFile : new File([attachmentFile as Blob], (attachmentFile as any).name || "attachment", { type: (attachmentFile as any).type || "application/octet-stream" });
        formData.append("images", rawFile, rawFile.name);
    }

    const url = API_URL + "ai-review/gp-chat-with-images";
    const request = {
        method: "POST",
        headers: {
            Authorization: "bearer " + KeycloakService.GetAccessToken(),
            "projectID": projectID,
        },
        body: formData,
    };
    const data = await fetchResults(url, request as unknown as RequestInit);
    return data;
}

async function clearGpCache(personID?: string): Promise<{status: string}> {
    let url = window.location.protocol + window.location.host + API_URL + "ai-review/gp-cache/clear";
    if (personID) {
        const searchURL = new URL(url);
        searchURL.searchParams.set("personID", personID);
        searchURL.searchParams.set("projectID", projectID || "");
        url = searchURL.toString();
    }
    const request = buildGETRequest();
    return await fetchResults(url, request);
}

async function sendGpChatMode(personID: string, conversationHistory: ChatMessage[]): Promise<AiReviewResponse> {
    await getUserProjectID();

    const body: any = { personID, conversationHistory };

    const request = {
        method: "POST",
        headers: {
            Accept: "*/*",
            Authorization: "bearer " + KeycloakService.GetAccessToken(),
            "projectID": projectID,
            "Content-Type": "application/json",
        },
        body: JSON.stringify(body),
    };
    const searchURL = new URL(window.location.protocol + window.location.host + API_URL + "ai-review/gp-chat-mode");
    const data = await fetchResults(searchURL.toString(), request as unknown as RequestInit);
    return data;
}

async function sendGpChatModeWithAttachment(personID: string, conversationHistory: ChatMessage[], attachmentFile: File | null): Promise<AiReviewResponse> {
    await getUserProjectID();

    const formData = new FormData();
    formData.append("personID", personID);
    formData.append("conversationHistory", JSON.stringify(conversationHistory));

    if (attachmentFile) {
        const rawFile = attachmentFile instanceof File ? attachmentFile : new File([attachmentFile as Blob], (attachmentFile as any).name || "attachment", { type: (attachmentFile as any).type || "application/octet-stream" });
        formData.append("images", rawFile, rawFile.name);
    }

    const url = API_URL + "ai-review/gp-chat-mode-with-images";
    const request = {
        method: "POST",
        headers: {
            Authorization: "bearer " + KeycloakService.GetAccessToken(),
            "projectID": projectID,
        },
        body: formData,
    };
    const data = await fetchResults(url, request as unknown as RequestInit);
    return data;
}

export default ApiPopulation;
