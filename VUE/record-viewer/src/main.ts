import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import router from "./routers";
import KeycloakService from "./KeycloakService";
import "./style.css";
import "./fontawesome.css";
import PrimeVue from 'primevue/config'
import Button from 'primevue/button'
import ToggleButton from 'primevue/togglebutton'
import InputText from 'primevue/inputtext'
import FloatLabel from "primevue/floatlabel"
import Toolbar from 'primevue/toolbar'
import OverlayPanel from 'primevue/overlaypanel'
import SelectButton from 'primevue/selectbutton'
import Menu from "primevue/menu"
import Panel from 'primevue/panel'
import Dialog from 'primevue/dialog'
import Accordion from 'primevue/accordion';
import AccordionTab from 'primevue/accordiontab';
import Checkbox from 'primevue/checkbox';
import Card from 'primevue/card'
import FieldSet from 'primevue/fieldset'
import Toast from 'primevue/toast'
import ToastService from 'primevue/toastservice'
import TreeTable from 'primevue/treetable'
import Divider from 'primevue/divider'
import TabMenu from 'primevue/tabmenu'
import TabView from 'primevue/tabview'
import TabPanel from 'primevue/tabpanel'
import ScrollPanel from 'primevue/scrollpanel'
import Splitter from 'primevue/splitter';
import SplitterPanel from 'primevue/splitterpanel';
import Column from 'primevue/column'
import DataTable from 'primevue/datatable';
import ColumnGroup from 'primevue/columngroup';   // optional
import Row from 'primevue/row';                   // optional
import Chart from 'primevue/chart';
import InputSwitch from 'primevue/inputswitch';
import ProgressSpinner from 'primevue/progressspinner';
import Calendar from 'primevue/calendar';
import Menubar from 'primevue/menubar';
import Tooltip from 'primevue/tooltip';
import Tree from 'primevue/tree';
import Tag from 'primevue/tag';
import Textarea from 'primevue/textarea';

const app = () => {
    createApp(App)
        .use(createPinia())
        .use(PrimeVue)
        .use(router)
        .use(ToastService)
        .component('Accordion', Accordion)
        .component('AccordionTab', AccordionTab)
        .component('Button', Button)
        .component('Calendar', Calendar)
        .component('Card', Card)
        .component('Chart', Chart)
        .component('Checkbox', Checkbox)
        .component('Column', Column)
        .component('ColumnGroup', ColumnGroup)
        .component('DataTable', DataTable)
        .component('Dialog', Dialog)
        .component('Divider', Divider)
        .component('FieldSet', FieldSet)
        .component('FloatLabel', FloatLabel)
        .component('InputSwitch', InputSwitch)
        .component('InputText', InputText)
        .component('Menu', Menu)
        .component('Menubar', Menubar)
        .component('OverlayPanel', OverlayPanel)
        .component('Panel', Panel)
        .component('ProgressSpinner', ProgressSpinner)
        .component('Row', Row)
        .component('ScrollPanel', ScrollPanel)
        .component('SelectButton', SelectButton)
        .component('Splitter', Splitter)
        .component('SplitterPanel', SplitterPanel)
        .component('TabMenu', TabMenu)
        .component('TabPanel', TabPanel)
        .component('TabView', TabView)
        .component('Tag', Tag)
        .component('Textarea', Textarea)
        .component('Toast', Toast)
        .component('ToggleButton', ToggleButton)
        .component('Toolbar', Toolbar)
        .component('Tree', Tree)
        .component('TreeTable', TreeTable)
        .directive('Tooltip', Tooltip)
        .mount('#app');
};

KeycloakService.CallLogin(app);
