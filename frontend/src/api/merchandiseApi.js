import client from './client'

export const getMerchandise = () => client.get('/merchandise')
export const getMerchandiseById = (id) => client.get(`/merchandise/${id}`)
export const createMerchandise = (data) => client.post('/merchandise', data)
export const updateMerchandise = (id, data) => client.put(`/merchandise/${id}`, data)
export const adjustQuantity = (id, delta) => client.patch(`/merchandise/${id}/quantity`, { delta })
export const deleteMerchandise = (id) => client.delete(`/merchandise/${id}`)
