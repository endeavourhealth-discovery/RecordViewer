<template>
<Button v-if="selectedProject" text aria-label="Role Selector" @click="toggleList">
  <i style="margin-right:8px;" class="fa-solid fa-briefcase"></i>
  {{selectedProject.projectName}}
  <OverlayPanel ref="rs" style="text-align:center; width:auto">
    <div v-for="(project, index) in userProjects" :key="index" style="display:inline">
      <div style="width:100%; display:flex">
        <Button text style="width:12%; margin:0;text-align:center;justify-content:center" @click="setDefaultProject(project)">
          <i v-if="project.default" class="fa-solid fa-star"/>
          <i v-else class="fa-regular fa-star"/>
        </Button>
        <Button text style="width:88%" @click="setSelectedProject(project)">{{project.projectName}}</Button>
      </div>
    </div>
  </OverlayPanel>
</Button>
</template>

<script>

import UserManager from "@/API/UserManager/UserManager";
import {ref} from "vue";

export default {
  name: 'RoleSelector',
  props:{
    selectedProject: {type: null}
  },
  data() {
    return {
      rs: ref(),
      userName: null,
      userProjects: [],
      userProfile: [],
    }
  },
  methods: {
    toggleList(event){
      this.$refs.rs.toggle(event);
    },
    async setDefaultProject(newProject) {
      await UserManager.ChangeDefaultProject(newProject.id, this.selectedProject.id);
      this.userProjects = await UserManager.GetUserProjects()
    },
    async setSelectedProject(newProject) {
      await UserManager.SetSelectedProject(newProject)
      this.$emit("selectedProjectChanged")
    }
  },
  async mounted() {
    this.userName = UserManager.GetUserName()
    this.userProjects = await UserManager.GetUserProjects()

  },
}

</script>

<style scoped>
</style>
