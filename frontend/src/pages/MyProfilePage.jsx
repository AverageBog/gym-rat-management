import { useEffect, useState, useRef } from 'react'
import { getMemberById, updateContact, updatePayment } from '../api/membersApi'
import { useAuth } from '../context/AuthContext'
import LoadingSpinner from '../components/common/LoadingSpinner'
import StatusBadge from '../components/common/StatusBadge'

export default function MyProfilePage() {
  const { user } = useAuth()
  const memberId = user?.memberId

  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [contactForm, setContactForm] = useState(null)
  const [paymentForm, setPaymentForm] = useState(null)
  const [showCvv, setShowCvv] = useState(false)
  const [saving, setSaving] = useState(false)
  const [successMsg, setSuccessMsg] = useState(null)

  useEffect(() => { load() }, [memberId])

  async function load() {
    try {
      setLoading(true)
      const res = await getMemberById(memberId)
      setProfile(res.data)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  function openContactEdit() {
    setContactForm({ name: profile.name, phone: profile.phone || '' })
  }

  function openPaymentEdit() {
    setShowCvv(false)
    setPaymentForm({
      paymentMethod: profile.paymentMethod || '',
      cardNumber: profile.cardNumber
        ? profile.cardNumber.replace(/(.{4})/g, '$1 ').trim()
        : '',
      cardExpiryDate: profile.cardExpiryDate || '',
      cardCvv: profile.cardCvv || '',
      streetAddress: profile.streetAddress || '',
      aptUnit: profile.aptUnit || '',
      city: profile.city || '',
      state: profile.state || '',
      zipCode: profile.zipCode || '',
    })
  }

  async function saveContact(e) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const res = await updateContact(memberId, contactForm)
      setProfile(res.data)
      setContactForm(null)
      setSuccessMsg('Contact information updated.')
      setTimeout(() => setSuccessMsg(null), 3000)
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  async function savePayment(e) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    const digits = paymentForm.cardNumber.replace(/\s/g, '')
    if (digits && !/^\d{13,19}$/.test(digits)) {
      setError('Card number must be 13–19 digits.')
      setSaving(false)
      return
    }
    if (paymentForm.cardExpiryDate && !/^(0[1-9]|1[0-2])\/\d{2}$/.test(paymentForm.cardExpiryDate)) {
      setError('Expiry date must be in MM/YY format.')
      setSaving(false)
      return
    }
    if (paymentForm.cardCvv && !/^\d{3,4}$/.test(paymentForm.cardCvv)) {
      setError('CVV must be 3 or 4 digits.')
      setSaving(false)
      return
    }
    try {
      const res = await updatePayment(memberId, { ...paymentForm, cardNumber: digits })
      setProfile(res.data)
      setPaymentForm(null)
      setShowCvv(false)
      setSuccessMsg('Payment information updated.')
      setTimeout(() => setSuccessMsg(null), 3000)
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <LoadingSpinner />

  return (
    <div className="page">
      <h1>My Profile</h1>

      {error && <div className="error-banner">{error} <button onClick={() => setError(null)}>✕</button></div>}
      {successMsg && <div className="success-banner">{successMsg}</div>}

      {/* Membership overview */}
      <div className="detail-card">
        <div className="detail-header">
          <div>
            <h2 style={{ margin: 0 }}>{profile.name}</h2>
            <p style={{ margin: '0.25rem 0 0', color: '#94a3b8', fontSize: '0.875rem' }}>{profile.email}</p>
          </div>
          <StatusBadge status={profile.status} />
        </div>
        <div className="detail-meta" style={{ marginTop: '0.75rem' }}>
          <span>Plan: <strong>{profile.membershipPlanName || '—'}</strong></span>
          {profile.nextPaymentDate && (
            <span>Next payment: <strong style={{ color: '#e94560' }}>{profile.nextPaymentDate}</strong></span>
          )}
        </div>
      </div>

      <div className="detail-grid">
        {/* Contact info */}
        <section className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <h2 style={{ margin: 0 }}>Contact Information</h2>
            {!contactForm && (
              <button className="btn btn-sm btn-secondary" onClick={openContactEdit}>Edit</button>
            )}
          </div>

          {contactForm ? (
            <form onSubmit={saveContact}>
              <div className="form-group">
                <label>Name</label>
                <input
                  required
                  value={contactForm.name}
                  onChange={e => setContactForm({ ...contactForm, name: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Phone</label>
                <input
                  value={contactForm.phone}
                  onChange={e => setContactForm({ ...contactForm, phone: e.target.value })}
                />
              </div>
              <div className="modal-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setContactForm(null)}>Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={saving}>Save</button>
              </div>
            </form>
          ) : (
            <dl className="profile-dl">
              <div><dt>Name</dt><dd>{profile.name}</dd></div>
              <div><dt>Email</dt><dd>{profile.email}</dd></div>
              <div><dt>Phone</dt><dd>{profile.phone || '—'}</dd></div>
            </dl>
          )}
        </section>

        {/* Payment info */}
        <section className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <h2 style={{ margin: 0 }}>Payment Information</h2>
            {!paymentForm && (
              <button className="btn btn-sm btn-secondary" onClick={openPaymentEdit}>Edit</button>
            )}
          </div>

          {paymentForm ? (
            <form onSubmit={savePayment}>
              <div className="form-group">
                <label>Payment Method</label>
                <select
                  value={paymentForm.paymentMethod}
                  onChange={e => setPaymentForm({ ...paymentForm, paymentMethod: e.target.value })}
                >
                  <option value="">Select…</option>
                  <option value="Visa">Visa</option>
                  <option value="Mastercard">Mastercard</option>
                  <option value="Amex">Amex</option>
                  <option value="Discover">Discover</option>
                  <option value="Bank Transfer">Bank Transfer</option>
                </select>
              </div>
              <div className="form-group">
                <label>Card Number</label>
                <input
                  type="text"
                  inputMode="numeric"
                  maxLength={19}
                  placeholder="1234 5678 9012 3456"
                  value={paymentForm.cardNumber}
                  onChange={e => {
                    const digits = e.target.value.replace(/\D/g, '').slice(0, 16)
                    const formatted = digits.replace(/(.{4})/g, '$1 ').trim()
                    setPaymentForm({ ...paymentForm, cardNumber: formatted })
                  }}
                />
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <div className="form-group" style={{ margin: 0 }}>
                  <label>Expiry Date</label>
                  <input
                    type="text"
                    maxLength={5}
                    placeholder="MM/YY"
                    value={paymentForm.cardExpiryDate}
                    onChange={e => {
                      let v = e.target.value.replace(/\D/g, '').slice(0, 4)
                      if (v.length > 2) v = v.slice(0, 2) + '/' + v.slice(2)
                      setPaymentForm({ ...paymentForm, cardExpiryDate: v })
                    }}
                  />
                </div>
                <div className="form-group" style={{ margin: 0 }}>
                  <label>CVV</label>
                  <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                    <input
                      type={showCvv ? 'text' : 'password'}
                      inputMode="numeric"
                      maxLength={4}
                      placeholder="•••"
                      style={{ flex: 1 }}
                      value={paymentForm.cardCvv}
                      onChange={e => setPaymentForm({ ...paymentForm, cardCvv: e.target.value.replace(/\D/g, '').slice(0, 4) })}
                    />
                    <button
                      type="button"
                      className="btn btn-sm btn-secondary"
                      style={{ whiteSpace: 'nowrap', padding: '0.35rem 0.6rem' }}
                      onClick={() => setShowCvv(v => !v)}
                      title={showCvv ? 'Hide CVV' : 'Show CVV'}
                    >
                      {showCvv ? 'Hide' : 'Show'}
                    </button>
                  </div>
                </div>
              </div>
              <div className="form-group">
                <label>Street Address</label>
                <input
                  placeholder="123 Main St"
                  value={paymentForm.streetAddress}
                  onChange={e => setPaymentForm({ ...paymentForm, streetAddress: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Apt / Unit Number <span style={{ color: '#94a3b8', fontWeight: 400 }}>(optional)</span></label>
                <input
                  placeholder="Apt 4B"
                  value={paymentForm.aptUnit}
                  onChange={e => setPaymentForm({ ...paymentForm, aptUnit: e.target.value })}
                />
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr auto auto', gap: '0.75rem' }}>
                <div className="form-group" style={{ margin: 0 }}>
                  <label>City</label>
                  <input
                    placeholder="Austin"
                    value={paymentForm.city}
                    onChange={e => setPaymentForm({ ...paymentForm, city: e.target.value })}
                  />
                </div>
                <div className="form-group" style={{ margin: 0, minWidth: '80px' }}>
                  <label>State</label>
                  <input
                    placeholder="TX"
                    maxLength={50}
                    value={paymentForm.state}
                    onChange={e => setPaymentForm({ ...paymentForm, state: e.target.value })}
                  />
                </div>
                <div className="form-group" style={{ margin: 0, minWidth: '100px' }}>
                  <label>Zip Code</label>
                  <input
                    placeholder="78701"
                    maxLength={10}
                    value={paymentForm.zipCode}
                    onChange={e => setPaymentForm({ ...paymentForm, zipCode: e.target.value.replace(/[^\d-]/, '') })}
                  />
                </div>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setPaymentForm(null)}>Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={saving}>Save</button>
              </div>
            </form>
          ) : (
            <dl className="profile-dl">
              <div><dt>Method</dt><dd>{profile.paymentMethod || '—'}</dd></div>
              <div>
                <dt>Card Number</dt>
                <dd>{profile.cardNumber ? `•••• •••• •••• ${profile.cardNumber.slice(-4)}` : '—'}</dd>
              </div>
              <div><dt>Expiry</dt><dd>{profile.cardExpiryDate || '—'}</dd></div>
              <div><dt>CVV</dt><dd>{profile.cardCvv ? '•••' : '—'}</dd></div>
              <div>
                <dt>Billing Address</dt>
                <dd>
                  {profile.streetAddress ? (
                    <>
                      {profile.streetAddress}
                      {profile.aptUnit && `, ${profile.aptUnit}`}
                      {(profile.city || profile.state || profile.zipCode) && (
                        <><br />{[profile.city, profile.state, profile.zipCode].filter(Boolean).join(', ')}</>
                      )}
                    </>
                  ) : '—'}
                </dd>
              </div>
            </dl>
          )}
        </section>
      </div>
    </div>
  )
}
