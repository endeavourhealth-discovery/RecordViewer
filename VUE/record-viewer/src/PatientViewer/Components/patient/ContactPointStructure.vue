<template>
  <div class="telecom">
    <div class="homePhone">homePhone:</div>
    <div class="mob">mobile:</div>
    <div class="email">email:</div>
  </div>
</template>

<script>
export default {
  name: "ContactPointStructure",
  props: ["contactPoint", "index"],
};
</script>

<style scoped></style>
