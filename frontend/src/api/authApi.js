import client from './client'

export const login = (email, password) => client.post('/auth/login', { email, password })
export const createAdmin = (email, password) => client.post('/auth/admin', { email, password })
