self.addEventListener('push', (event) => {
  let data = { title: '通知', body: '', url: '/calendar' };
  try {
    if (event.data) {
      data = JSON.parse(event.data.text());
    }
  } catch (e) {
    data = { title: '通知', body: event.data ? event.data.text() : '', url: '/calendar' };
  }

  event.waitUntil(
    self.registration.showNotification(data.title || '通知', {
      body: data.body || '',
      data: { url: data.url || '/calendar' }
    })
  );
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const targetUrl = (event.notification.data && event.notification.data.url) || '/calendar';
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((windowClients) => {
      for (const client of windowClients) {
        if ('focus' in client) {
          client.navigate(targetUrl);
          return client.focus();
        }
      }
      if (clients.openWindow) {
        return clients.openWindow(targetUrl);
      }
      return undefined;
    })
  );
});
