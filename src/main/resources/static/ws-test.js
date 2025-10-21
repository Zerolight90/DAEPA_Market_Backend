(function () {
    const logEl = document.getElementById('log');
    const log = (...a)=>{ logEl.textContent += a.map(x => typeof x==='string'?x:JSON.stringify(x)).join(' ')+"\n"; logEl.scrollTop = logEl.scrollHeight; };

    let client;

    document.getElementById('connect').addEventListener('click', () => {
        const url = document.getElementById('wsUrl').value;
        const who = document.getElementById('who').value; // "1:구매자1" or "2:판매자"
        const sub = document.getElementById('subDest').value;
        const socket = new SockJS(url);

        client = Stomp.over(socket);
        client.debug = (m)=>log('[STOMP]', m);

        client.connect(
            { Authorization: 'Bearer ' + who }, // <- 헤더로 사용자 전달
            () => {
                log('CONNECTED as', who);
                document.getElementById('send').disabled = false;

                client.subscribe(sub, (msg) => {
                    try { log('RECV:', JSON.parse(msg.body)); }
                    catch { log('RECV:', msg.body); }
                });
            },
            (err) => log('CONNECT ERROR:', err)
        );
    });

    document.getElementById('send').addEventListener('click', () => {
        const dest = document.getElementById('sendDest').value;
        const payload = document.getElementById('payload').value;
        client.send(dest, {'content-type':'application/json'}, payload);
        log('SENT:', payload);
    });
})();
