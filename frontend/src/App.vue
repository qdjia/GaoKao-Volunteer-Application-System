<template>
  <router-view />
</template>

<script setup>
import { useCandidatePresence } from './utils/candidatePresence'
useCandidatePresence()
</script>

<style>
:root { font-family: "Segoe UI", "Microsoft YaHei", "PingFang SC", sans-serif; font-size: 14px; line-height: 1.5; letter-spacing: 0; color: #263b40; }
* { box-sizing: border-box; }
body { margin: 0; }
button, input, select, textarea { font-family: inherit; }
</style>
