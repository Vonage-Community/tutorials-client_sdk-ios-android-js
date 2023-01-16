'use strict';

const port = 3000;
const subdomain = 'SUBDOMAIN';

const express = require('express')
const app = express();
app.use(express.json());

app.get('/voice/answer', (req, res) => {
  console.log('NCCO request:');
  const callee = JSON.parse(req.query.custom_data).callee;
  console.log(`  - caller: ${req.query.from_user}`);
  console.log(`  - callee: ${callee}`);
  console.log('---');
  res.json([ 
    { 
      "action": "talk", 
      "text": "Please wait while we connect you."
    },
    { 
      "action": "connect", 
      "endpoint": [ 
        { "type": "app", "user": callee } 
      ]
    }
  ]);
});

app.all('/voice/event', (req, res) => {
  console.log('EVENT:');
  console.dir(req.body);
  console.log('---');
  res.sendStatus(200);
});

if(subdomain == "SUBDOMAIN") {
  console.log('\n\t🚨🚨🚨 Please change the SUBDOMAIN value');
  return false;
}
app.listen(3000);

const localtunnel = require('localtunnel');
(async () => {
  const tunnel = await localtunnel({ 
      subdomain: subdomain, 
      port: 3000
    });
  console.log(`App available at: ${tunnel.url}`);
})();
