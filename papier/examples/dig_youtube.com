[antoine@M1530 ~]$ dig -t soa youtube.com

; <<>> DiG 9.9.4-P2-RedHat-9.9.4-12.P2.fc20 <<>> -t soa youtube.com
;; global options: +cmd
;; Got answer:
;; ->>HEADER<<- opcode: QUERY, status: NOERROR, id: 56838
;; flags: qr rd ra; QUERY: 1, ANSWER: 1, AUTHORITY: 0, ADDITIONAL: 1

;; OPT PSEUDOSECTION:
; EDNS: version: 0, flags:; udp: 1460
;; QUESTION SECTION:
;youtube.com.			IN	SOA

;; ANSWER SECTION:
youtube.com.		3600	IN	SOA	ns1.google.com. dns-admin.google.com. 2014042900 10800 3600 604800 600
