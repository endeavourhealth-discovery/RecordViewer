import axios from 'axios';

const baseURL =
    import.meta.env.PROD
        ? '/record-viewer/api'
        : '/record-viewer/api';

const axiosInstance = axios.create({ baseURL });

export default axiosInstance;
