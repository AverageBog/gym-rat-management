import client from './client'

export const getPlans = () => client.get('/plans')
export const getPlanById = (id) => client.get(`/plans/${id}`)
export const createPlan = (data) => client.post('/plans', data)
export const updatePlan = (id, data) => client.put(`/plans/${id}`, data)
export const deletePlan = (id) => client.delete(`/plans/${id}`)
