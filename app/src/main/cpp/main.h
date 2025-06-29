#define VERSION "17.1"
union sockaddr_u;

int get_default_ttl(void);

int get_addr(const char *str, union sockaddr_u *addr);

void *add(void **root, int *n, size_t ss);

void clear_params(void);

char *ftob(const char *str, ssize_t *sl);

char *data_from_str(const char *str, ssize_t *size);

size_t parse_cform(char *buffer, size_t blen, const char *str, size_t slen);

struct mphdr *parse_hosts(char *buffer, size_t size);

struct mphdr *parse_ipset(char *buffer, size_t size);

int parse_offset(struct part *part, const char *str);

static const char help_text[] = {
        "    -i, --ip, <ip>            Listening IP, default 0.0.0.0\n"
        "    -p, --port <num>          Listening port, default 1080\n"
        #ifdef DAEMON
        "    -D, --daemon              Daemonize\n"
    "    -w, --pidfile <filename>  Write PID to file\n"
        #endif
        #ifdef __linux__
        "    -E, --transparent         Transparent proxy mode\n"
        #endif
        "    -c, --max-conn <count>    Connection count limit, default 512\n"
        "    -N, --no-domain           Deny domain resolving\n"
        "    -U, --no-udp              Deny UDP association\n"
        "    -I  --conn-ip <ip>        Connection binded IP, default ::\n"
        "    -b, --buf-size <size>     Buffer size, default 16384\n"
        "    -x, --debug <level>       Print logs, 0, 1 or 2\n"
        "    -g, --def-ttl <num>       TTL for all outgoing connections\n"
        // desync options
        #ifdef TCP_FASTOPEN_CONNECT
        "    -F, --tfo                 Enable TCP Fast Open\n"
        #endif
        "    -A, --auto <t,r,s,n>      Try desync params after this option\n"
        "                              Detect: torst,redirect,ssl_err,none\n"
        "    -L, --auto-mode <0-3>     Mode: 1 - post_resp, 2 - sort, 3 - 1+2\n"
        "    -u, --cache-ttl <sec>     Lifetime of cached desync params for IP\n"
        "    -y, --cache-dump <file|-> Dump cache to file or stdout\n"
        #ifdef TIMEOUT_SUPPORT
        "    -T, --timeout <sec>       Timeout waiting for response, after which trigger auto\n"
        #endif
        "    -K, --proto <t,h,u,i>     Protocol whitelist: tls,http,udp,ipv4\n"
        "    -H, --hosts <file|:str>   Hosts whitelist, filename or :string\n"
        "    -j, --ipset <file|:str>   IP whitelist\n"
        "    -V, --pf <port[-portr]>   Ports range whitelist\n"
        "    -R, --round <num[-numr]>  Number of request to which desync will be applied\n"
        "    -s, --split <pos_t>       Position format: offset[:repeats:skip][+flag1[flag2]]\n"
        "                              Flags: +s - SNI offset, +h - HTTP host offset, +n - null\n"
        "                              Additional flags: +e - end, +m - middle\n"
        "    -d, --disorder <pos_t>    Split and send reverse order\n"
        "    -o, --oob <pos_t>         Split and send as OOB data\n"
        "    -q, --disoob <pos_t>      Split and send reverse order as OOB data\n"
        #ifdef FAKE_SUPPORT
        "    -f, --fake <pos_t>        Split and send fake packet\n"
        #ifdef __linux__
        "    -S, --md5sig              Add MD5 Signature option for fake packets\n"
        #endif
        "    -n, --fake-sni <str>      Change SNI in fake\n"
        "                              Replaced: ? - rand let, # - rand num, * - rand let/num\n"
        #endif
        "    -t, --ttl <num>           TTL of fake packets, default 8\n"
        "    -O, --fake-offset <pos_t> Fake data start offset\n"
        "    -l, --fake-data <f|:str>  Set custom fake packet\n"
        "    -Q, --fake-tls-mod <r,o>  Modify fake TLS CH: rand,orig\n"
        "    -e, --oob-data <char>     Set custom OOB data\n"
        "    -M, --mod-http <h,d,r>    Modify HTTP: hcsmix,dcsmix,rmspace\n"
        "    -r, --tlsrec <pos_t>      Make TLS record at position\n"
        "    -a, --udp-fake <count>    UDP fakes count, default 0\n"
        #ifdef __linux__
        "    -Y, --drop-sack           Drop packets with SACK extension\n"
#endif
};