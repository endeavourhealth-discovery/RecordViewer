<template>
  <div style="display:flex">
    <div style="height:3rem; margin:1rem 2rem 0 2rem">
      <i :class="icon" style="font-size:3rem; margin-right:10px; display:inline"></i>
    </div>

    <div style="display:inline; text-align:left; padding:0; margin:0">
      <h4 style="padding-bottom:0; margin-bottom:0">{{ title }}</h4>
      <p style="text-align:left; padding:0; margin:0 0 1rem 0">{{ subtitle }}</p>
    </div>
    <ProgressSpinner v-show="searching" style="width:50px; height:50px; margin-top: 20px; float:right" strokeWidth="5"></ProgressSpinner>
  </div>
</template>

<script>
export default {
    name: "PageHeader",
    props: {
        searching: Boolean,
        title: String,
        subtitle: String,
        icon: String,
    },
};
</script>
