<template>
  <q-page class="q-pa-lg">
    <div class="text-h5">
      Студент {{ login }}
    </div>
  </q-page>
</template>

<script setup>
import { useAuthStore } from 'src/stores/auth'
const auth = useAuthStore()
const login = auth.login
</script>
