<template>
  <div>
    <LineChart :data="chartData" width="600px" height="400px" />
  </div>
</template>

<script>
import LineChart from "@/views/charts/LineChart.vue";

export default {
  components: { LineChart },
  data() {
    return {
      chartData: [
        { name: "Jan", value: 820 },
        { name: "Feb", value: 932 },
        { name: "Mar", value: 901 },
        { name: "Apr", value: 934 },
        { name: "May", value: 1290 },
        { name: "Jun", value: 1330 },
        { name: "Jul", value: 1320 },
      ],
    };
  },
};
</script>
