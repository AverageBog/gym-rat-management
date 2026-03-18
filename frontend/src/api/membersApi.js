import client from './client'

export const getMembers = (status) => client.get('/members', { params: status ? { status } : {} })
export const getMemberById = (id) => client.get(`/members/${id}`)
export const createMember = (data) => client.post('/members', data)
export const updateMember = (id, data) => client.put(`/members/${id}`, data)
export const updateMemberStatus = (id, status) => client.patch(`/members/${id}/status`, { status })
export const deleteMember = (id) => client.delete(`/members/${id}`)
