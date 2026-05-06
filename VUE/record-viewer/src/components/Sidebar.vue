<template>
  <aside id="sidebar" :style="{ width: sidebarWidth + 'px' }"
         @mouseover="OpenMenu" @mouseleave="CloseMenu">
    <div class="header">
      <div class="logo">
        <img :src="logoSrc" alt="App Logo" style="width:3rem; color: white">
      </div>
      <div class="title">
        <h2 :style="titleStyle">{{ title }}</h2>
        <ToggleButton aria-label="pin-sidebar" class="pinButton" v-model="pinned"
                      role="checkbox" :onLabel="''" :offLabel='""'
                      onIcon="fa-solid fa-thumbtack" offIcon="fa-solid fa-thumbtack" />
      </div>
    </div>
    <div class="content">
      <ul>
        <li role="button" v-for="item in items" :key="item.label" @click="item.command()">
          <div style="width:60px; ">
            <i :class="item.icon"></i>
          </div>
          <div style="width:200px; margin: 0 0 0 6px">
            <p :style="textStyle">{{ item.label }}</p>
          </div>
        </li>
      </ul>
    </div>
  </aside>

</template>

<script>
import AccessControl from "@/API/AccessManager";
import logo from '@/assets/logo.png';
export default {
  name: "SideBar",
  props: {
    title: {
      type: String,
      default: "Sidebar"
    },
    textStyle: {
      type: String,
      default: ""
    },
    titleStyle: {
      type: String,
      default: ""
    },
    logoSrc: {
      type: String,
      default: "@/assets/logo"
    },
    items: {
      type: Array,
      required: true
    }
  },
  data() {
    return {
      minWidth:60,
      maxWidth:260,
      sidebarWidth: 260,
      isExpanded: true,
      pinned: true
    };
  },
  async mounted() {
    window.addEventListener('resize', this.handleResize);
    for (const item of this.items) {
      if (item.routeName) {
        item.allowAccess = await AccessControl.CheckRoleAccess(
            routes.find(x => x.name === item.routeName).accessRoles[0]
        );
      } else {
        item.allowAccess = false;
      }
    }
    this.updateSize();
  },
  methods: {
    handleResize() {
      this.updateSize();
    },
    updateSize() {
      if (window.innerWidth <= 768) {
        this.minWidth = 0;
        this.sidebarWidth = 0;
      } else {
        this.minWidth = 60;
        this.sidebarWidth = this.pinned ? this.maxWidth : this.minWidth;
      }
    },
    OpenMenu() {
      this.sidebarWidth = this.maxWidth;
      this.isExpanded = true;
    },
    CloseMenu() {
      if (!this.pinned) {
        this.sidebarWidth = window.innerWidth <= 768 ? 0 : this.minWidth;
        this.isExpanded = false;
      }
    },
    noAccess(){
      this.$toast.add({
        severity: 'error',
        summary: 'You do not have access to this module',
        detail: 'If you believe this is incorrect, please contact your system administrator.',
        life: 8000,
      });
    },

    pinSidebar() {
      if (this.pinned) {
        this.OpenMenu();
      } else {
        this.sidebarWidth = this.minWidth;
        this.isExpanded = false;
      }
      this.$emit("pinned");
    }
  },
  watch: {
    pinned(val) {
      if (val) {
        this.OpenMenu();
      } else {
        this.sidebarWidth = this.minWidth;
        this.isExpanded = false;
      }
      this.$emit("pinned", val);
    }
  }
};
</script>


<style scoped>
#sidebar{
  position:fixed;
  height:103vh;
  overflow:hidden;
  background-color:#5a7999;
  overflow-y: auto; /* Enable vertical scrollbar if needed */
  white-space: nowrap;
  transition: all .15s ease;
  color: white;
  z-index:1000;
}
.header{
  background-color:#04466B;
  margin:5px 0 0 0;
  padding: 0 0 0 0;
  height:10vh;
  color: white;
}
.fa-thumbtack{
  padding: 0px;
  margin:0px;
  height:0px;
  width:0px;
}
.logo{
  width:60px;
  margin:0px;
  padding: 12px 0 0 0;
}
.title{
  font-size:13px;
  display: flex;
  margin: 7px 0 0 0;
  text-align: left;
}

.content{
  background-color:#5a7999;
}

i{
  font-size:2.5rem;
}

li{
  list-style-type: none;
  display:flex;
  padding: 1.166vh 0 1.166vh 0;
  margin:0;
  cursor: pointer;
}

li:hover{
  background-color: #4a6989;
}

li i{
  margin: 0 20px 0 20px;
  padding: 0;
  font-size:1.2rem;
}
li p{
  padding:0;
  margin:3px 0 0 0;
  white-space: nowrap;
  text-align:left;
}
ul{
  padding:0;
  margin:0;
}

.button{

}

.pinButton{
  width: 32px; height: 32px; margin: 10px 0 0 16px;
  display:block;
}

@media (max-width:768px){
  .pinButton{
    display: none;
  }
}
</style>
