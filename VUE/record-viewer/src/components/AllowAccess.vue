<template>
  <Card>
    <template #header>
      <PageHeader :title="title" :subtitle="subtitle" icon="fa-solid fa-ban"></PageHeader>
    </template>
    <template #content>
      <div style="display:flex; flex-direction:row; justify-content:center" v-if="intendedProject"><p><b>{{intendedProject.projectName}}</b> has access to this page</p>
        <Button style="margin:8px; padding:8px" @click="changeProject">Use {{intendedProject.projectName}}</Button></div>
      <p v-else style="display:flex; flex-direction:row; justify-content:center">If you believe this is incorrect, please contact your system administrator.</p>
    </template>
  </Card>
</template>

<script>

import {ref} from "vue";
import UserManager from "@/API/UserManager/UserManager";
import PageHeader from "@/components/PageHeader.vue";

export default {
  name: 'AllowAccess',

  components: {
    PageHeader
  },
  data() {
    return {
      aa: ref(),
      title: "Access denied",
      subtitle: "",
      userName: null,
      selectedProject: null,
      intendedProject: null,
    }
  },
  methods: {
    async getPossibleCorrectProject(){
      const userProjects = await UserManager.GetUserProjects()
      for(let i = 0; i < userProjects.length; i++){
        const x = await UserManager.checkRolesAccessForProject(["record-viewer"], userProjects[i])
        if(x) {
          this.intendedProject = userProjects[i];
          break;
        }
      }
    },
    async changeProject(){
      UserManager.SetSelectedProject(this.intendedProject);
      this.$emit("selectedProjectChanged");
    }
  },
  async mounted() {
    this.userName = await UserManager.GetUserName()
    this.selectedProject = await UserManager.GetSelectedProject()
    this.subtitle="User " + this.userName + " can not access this page with role: " + this.selectedProject.projectName
    await this.getPossibleCorrectProject()
  },
}

</script>

<style scoped>
</style>
