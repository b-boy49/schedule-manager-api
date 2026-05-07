'use client';

import { useState } from 'react';
import { api } from '@/lib/api';
import { useRouter } from 'next/navigation';

export default function RegisterPage() {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const router = useRouter();

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await api('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ username, email, password })
    });
    router.push('/login');
  };

  return (
    <main style={{ maxWidth: 400, margin: '80px auto', padding: 24, background: '#17202a', borderRadius: 12 }}>
      <h1>Register</h1>
      <form onSubmit={onSubmit}>
        <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="Username" style={{ width: '100%', marginBottom: 12, padding: 10 }} />
        <input value={email} onChange={(e) => setEmail(e.target.value)} placeholder="Email" style={{ width: '100%', marginBottom: 12, padding: 10 }} />
        <input value={password} onChange={(e) => setPassword(e.target.value)} type="password" placeholder="Password" style={{ width: '100%', marginBottom: 12, padding: 10 }} />
        <button type="submit" style={{ width: '100%', padding: 10, background: '#1d9bf0', color: '#fff', border: 0, borderRadius: 8 }}>Create account</button>
      </form>
    </main>
  );
}
