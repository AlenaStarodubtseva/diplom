<template>
  <q-page class="q-pa-lg">
    <div class="text-h5">
      Секретарь факультета {{ facultyId }}
    </div>
  </q-page>
</template>

<script setup>
import { useAuthStore } from 'src/stores/auth'
const auth = useAuthStore()
const facultyId = auth.facultyId
</script>
