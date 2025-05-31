<template>
  <div class="third-party-page">
    <iframe
        src="http://iot.nomelin.top:18999"
        frameborder="0"
        class="third-party-iframe"
    ></iframe>
  </div>
</template>

<script>
export default {
  name: 'ThirdParty'
}
</script>

<style scoped>
.third-party-page {
  width: 100%;
  height: 100%;
  overflow: hidden;
}
.third-party-iframe {
  width: 100%;
  height: calc(100vh - 64px); /* 根据顶部导航栏高度调整 */
  border: none;
}
</style>
