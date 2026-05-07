'use client';

import { useState } from 'react';
import { api } from '@/lib/api';
import { useRouter } from 'next/navigation';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const router = useRouter();

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await api<{ accessToken: string }>('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password })
      });
      localStorage.setItem('accessToken', res.accessToken);
      router.push('/dm');
    } catch {
      setError('Login failed');
    }
  };

  return (
    <main style={{ maxWidth: 400, margin: '80px auto', padding: 24, background: '#17202a', borderRadius: 12 }}>
      <h1>Login</h1>
      <form onSubmit={onSubmit}>
        <input value={email} onChange={(e) => setEmail(e.target.value)} placeholder="Email" style={{ width: '100%', marginBottom: 12, padding: 10 }} />
        <input value={password} onChange={(e) => setPassword(e.target.value)} type="password" placeholder="Password" style={{ width: '100%', marginBottom: 12, padding: 10 }} />
        <button type="submit" style={{ width: '100%', padding: 10, background: '#1d9bf0', color: '#fff', border: 0, borderRadius: 8 }}>Sign in</button>
      </form>
      {error && <p>{error}</p>}
    </main>
  );
}
