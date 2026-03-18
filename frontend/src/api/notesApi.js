import client from './client'

export const getNotesByMember = (memberId) => client.get(`/notes/member/${memberId}`)
export const addNote = (memberId, content) => client.post(`/notes/member/${memberId}`, { content })
export const updateNote = (id, content) => client.put(`/notes/${id}`, { content })
export const deleteNote = (id) => client.delete(`/notes/${id}`)
