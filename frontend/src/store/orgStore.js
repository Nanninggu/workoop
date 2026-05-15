import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { orgApi } from '@/api/orgApi'

export const useOrgStore = defineStore('org', () => {
  const orgs       = ref([])
  const currentOrg = ref(JSON.parse(localStorage.getItem('coop-current-org') || 'null'))

  const hasOrg = computed(() => orgs.value.length > 0)

  async function fetchOrgs() {
    const res = await orgApi.list()
    orgs.value = res.data
    if (!currentOrg.value && orgs.value.length > 0) {
      setCurrentOrg(orgs.value[0])
    }
  }

  function setCurrentOrg(org) {
    currentOrg.value = org
    localStorage.setItem('coop-current-org', JSON.stringify(org))
  }

  async function createOrg(name) {
    const res = await orgApi.create(name)
    orgs.value.push(res.data)
    setCurrentOrg(res.data)
    return res.data
  }

  function clear() {
    orgs.value = []
    currentOrg.value = null
    localStorage.removeItem('coop-current-org')
  }

  return { orgs, currentOrg, hasOrg, fetchOrgs, setCurrentOrg, createOrg, clear }
})
