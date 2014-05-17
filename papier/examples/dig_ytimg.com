[antoine@M1530 ~]$ dig -t soa ytimg.com

; <<>> DiG 9.9.4-P2-RedHat-9.9.4-12.P2.fc20 <<>> -t soa ytimg.com
;; global options: +cmd
;; Got answer:
;; ->>HEADER<<- opcode: QUERY, status: NOERROR, id: 9882
;; flags: qr rd ra; QUERY: 1, ANSWER: 1, AUTHORITY: 0, ADDITIONAL: 1

;; OPT PSEUDOSECTION:
; EDNS: version: 0, flags:; udp: 1460
;; QUESTION SECTION:
;ytimg.com.			IN	SOA

;; ANSWER SECTION:
ytimg.com.		3600	IN	SOA	ns1.google.com. dns-admin.google.com. 2010120300 21600 3600 1209600 300
