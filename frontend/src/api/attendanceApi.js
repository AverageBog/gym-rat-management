import client from './client'

export const checkIn = (memberId) => client.post(`/attendance/checkin/${memberId}`)
export const getAttendanceByMember = (memberId) => client.get(`/attendance/member/${memberId}`)
export const getAllAttendance = (start, end) =>
  client.get('/attendance', { params: start && end ? { start, end } : {} })
export const deleteAttendance = (id) => client.delete(`/attendance/${id}`)
