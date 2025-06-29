#include <getopt.h>
#include <stdlib.h>
#include <string.h>

#include "byedpi/params.h"
#include "error.h"
#include "main.h"
#include "packets.h"
#include "utils.h"

struct params default_params;
extern const struct option options[46];

void reset_params(void) {
    clear_params();
    params = default_params;
}

static struct desync_params *add_group(struct desync_params *prev)
{
    struct desync_params *dp = calloc(1, sizeof(*prev));
    if (!dp) {
        return 0;
    }
    if (prev) {
        dp->prev = prev;
        prev->next = dp;
    }
    params.dp_n++;
    return dp;
}

int parse_args(int argc, char **argv)
{
    int optc = sizeof(options)/sizeof(*options);
    for (int i = 0, e = optc; i < e; i++)
        optc += options[i].has_arg;

    char opt[optc + 1];
    opt[optc] = 0;

    for (int i = 0, o = 0; o < optc; i++, o++) {
        opt[o] = options[i].val;
        for (int c = options[i].has_arg; c; c--) {
            o++;
            opt[o] = ':';
        }
    }
    //
    params.laddr.in.sin_port = htons(1080);
    if (!ipv6_support()) {
        params.baddr.sa.sa_family = AF_INET;
    }

    const char *pid_file = 0;
    bool daemonize = 0;

    int rez;
    int invalid = 0;

    long val = 0;
    char *end = 0;
    bool all_limited = 1;

    int curr_optind = 1;

    struct desync_params *dp = add_group(0);
    if (!dp) {
        reset_params();
        return -1;
    }
    params.dp = dp;

    while (!invalid && (rez = getopt_long(
            argc, argv, opt, options, 0)) != -1) {
        switch (rez) {

            case 'N':
                params.resolve = 0;
                break;
            case 'X':
                params.ipv6 = 0;
                break;
            case 'U':
                params.udp = 0;
                break;
            case 'G':
                params.http_connect = 1;
                break;
#ifdef __linux__
            case 'E':
                params.transparent = 1;
                break;
#endif

#ifdef DAEMON
                case 'D':
            daemonize = 1;
            break;
            
        case 'w':
            pid_file = optarg;
            break;
#endif
            case 'h':
                printf("%s", help_text);
                reset_params();
                return 0;
            case 'v':
                printf("%s\n", VERSION);
                reset_params();
                return 0;

            case 'i':
                if (get_addr(optarg, &params.laddr) < 0)
                    invalid = 1;
                break;

            case 'p':
                val = strtol(optarg, &end, 0);
                if (val <= 0 || val > 0xffff || *end)
                    invalid = 1;
                else
                    params.laddr.in.sin_port = htons(val);
                break;

            case 'I':
                if (get_addr(optarg, &params.baddr) < 0)
                    invalid = 1;
                break;

            case 'b':
                val = strtol(optarg, &end, 0);
                if (val <= 0 || val > INT_MAX/4 || *end)
                    invalid = 1;
                else
                    params.bfsize = val;
                break;

            case 'c':
                val = strtol(optarg, &end, 0);
                if (val <= 0 || val >= (0xffff/2) || *end)
                    invalid = 1;
                else
                    params.max_open = val;
                break;

            case 'x': //
                params.debug = strtol(optarg, 0, 0);
                if (params.debug < 0)
                    invalid = 1;
                break;

            case 'y': //
                params.cache_file = optarg;
                break;

                // desync options

            case 'F':
                params.tfo = 1;
                break;

            case 'L':
                end = optarg;
                while (end && !invalid) {
                    switch (*end) {
                        case '0':
                            break;
                        case '1':
                        case 'p':
                            params.auto_level |= AUTO_POST;
                            break;
                        case '2':
                        case 's':
                            params.auto_level |= AUTO_SORT;
                            break;
                        case 'r':
                            params.auto_level = 0;
                            break;
                        case '3':
                            params.auto_level |= (AUTO_POST | AUTO_SORT);
                            break;
                        default:
                            invalid = 1;
                            continue;
                    }
                    end = strchr(end, ',');
                    if (end) end++;
                }
                break;

            case 'A':
                if (optind < curr_optind) {
                    optind = curr_optind;
                    continue;
                }
                if (!(dp->hosts || dp->proto || dp->pf[0] || dp->detect || dp->ipset)) {
                    all_limited = 0;
                }
                dp = add_group(dp);
                if (!dp) {
                    reset_params();
                    return -1;
                }
                end = optarg;
                while (end && !invalid) {
                    switch (*end) {
                        case 't':
                            dp->detect |= DETECT_TORST;
                            break;
                        case 'r':
                            dp->detect |= DETECT_HTTP_LOCAT;
                            break;
                        case 'a':
                        case 's':
                            dp->detect |= DETECT_TLS_ERR;
                            break;
                        case 'n':
                            break;
                        default:
                            invalid = 1;
                            continue;
                    }
                    end = strchr(end, ',');
                    if (end) end++;
                }
                if (dp->detect) {
                    params.auto_level |= AUTO_RECONN;
                }
                dp->_optind = optind;
                break;

            case 'B':
                if (optind < curr_optind) {
                    continue;
                }
                if (*optarg == 'i') {
                    dp->pf[0] = htons(1);
                    continue;
                }
                val = strtol(optarg, &end, 0);
                struct desync_params *itdp = params.dp;

                while (itdp && itdp->id != val - 1) {
                    itdp = itdp->next;
                }
                if (!itdp)
                    invalid = 1;
                else {
                    curr_optind = optind;
                    optind = itdp->_optind;
                }
                break;

            case 'u':
                val = strtol(optarg, &end, 0);
                if (val <= 0 || *end)
                    invalid = 1;
                else
                    params.cache_ttl = val;
                break;

            case 'T':;
#ifdef __linux__
                float f = strtof(optarg, &end);
                val = (long)(f * 1000);
#else
                val = strtol(optarg, &end, 0);
#endif
                if (val <= 0 || (unsigned long)val > UINT_MAX || *end)
                    invalid = 1;
                else
                    params.timeout = val;
                break;

            case 'K':
                end = optarg;
                while (end && !invalid) {
                    switch (*end) {
                        case 't':
                            dp->proto |= IS_TCP | IS_HTTPS;
                            break;
                        case 'h':
                            dp->proto |= IS_TCP | IS_HTTP;
                            break;
                        case 'u':
                            dp->proto |= IS_UDP;
                            break;
                        case 'i':
                            dp->proto |= IS_IPV4;
                            break;
                        default:
                            invalid = 1;
                            continue;
                    }
                    end = strchr(end, ',');
                    if (end) end++;
                }
                break;

            case 'H':;
                if (dp->file_ptr) {
                    continue;
                }
                dp->file_ptr = ftob(optarg, &dp->file_size);
                if (!dp->file_ptr) {
                    uniperror("read/parse");
                    invalid = 1;
                    continue;
                }
                dp->hosts = parse_hosts(dp->file_ptr, dp->file_size);
                if (!dp->hosts) {
                    uniperror("parse_hosts");
                    reset_params();
                    return -1;
                }
                break;

            case 'j':;
                if (dp->ipset) {
                    continue;
                }
                ssize_t size;
                char *data = ftob(optarg, &size);
                if (!data) {
                    uniperror("read/parse");
                    invalid = 1;
                    continue;
                }
                dp->ipset = parse_ipset(data, size);
                if (!dp->ipset) {
                    uniperror("parse_ipset");
                    invalid = 1;
                }
                free(data);
                break;

            case 's':
            case 'd':
            case 'o':
            case 'q':
            case 'f':
                ;
                struct part *part = add((void *)&dp->parts,
                                        &dp->parts_n, sizeof(struct part));
                if (!part) {
                    reset_params();
                    return -1;
                }
                if (parse_offset(part, optarg)) {
                    invalid = 1;
                    break;
                }
                switch (rez) {
                    case 's': part->m = DESYNC_SPLIT;
                        break;
                    case 'd': part->m = DESYNC_DISORDER;
                        break;
                    case 'o': part->m = DESYNC_OOB;
                        break;
                    case 'q': part->m = DESYNC_DISOOB;
                        break;
                    case 'f': part->m = DESYNC_FAKE;
                }
                break;

            case 't':
                val = strtol(optarg, &end, 0);
                if (val <= 0 || val > 255 || *end)
                    invalid = 1;
                else
                    dp->ttl = val;
                break;

            case 'S':
                dp->md5sig = 1;
                break;

            case 'O':
                if (parse_offset(&dp->fake_offset, optarg)) {
                    invalid = 1;
                    break;
                } else dp->fake_offset.m = 1;
                break;

            case 'Q':
                end = optarg;
                while (end && !invalid) {
                    switch (*end) {
                        case 'r':
                            dp->fake_mod |= FM_RAND;
                            break;
                        case 'o':
                            dp->fake_mod |= FM_ORIG;
                            break;
                        default:
                            invalid = 1;
                            continue;
                    }
                    end = strchr(end, ',');
                    if (end) end++;
                }
                break;

            case 'n':;
                const char **p = add((void *)&dp->fake_sni_list,
                                     &dp->fake_sni_count, sizeof(optarg));
                if (!p) {
                    invalid = 1;
                    continue;
                }
                *p = optarg;
                break;

            case 'l':
                if (dp->fake_data.data) {
                    continue;
                }
                dp->fake_data.data = ftob(optarg, &dp->fake_data.size);
                if (!dp->fake_data.data) {
                    uniperror("read/parse");
                    invalid = 1;
                }
                break;

            case 'e':
                val = parse_cform(dp->oob_char, 1, optarg, strlen(optarg));
                if (val != 1) {
                    invalid = 1;
                }
                else dp->oob_char[1] = 1;
                break;

            case 'M':
                end = optarg;
                while (end && !invalid) {
                    switch (*end) {
                        case 'r':
                            dp->mod_http |= MH_SPACE;
                            break;
                        case 'h':
                            dp->mod_http |= MH_HMIX;
                            break;
                        case 'd':
                            dp->mod_http |= MH_DMIX;
                            break;
                        default:
                            invalid = 1;
                            continue;
                    }
                    end = strchr(end, ',');
                    if (end) end++;
                }
                break;

            case 'r':
                part = add((void *)&dp->tlsrec,
                           &dp->tlsrec_n, sizeof(struct part));
                if (!part) {
                    reset_params();
                    return -1;
                }
                if (parse_offset(part, optarg)
                    || part->pos > 0xffff) {
                    invalid = 1;
                    break;
                }
                break;

            case 'a':
                val = strtol(optarg, &end, 0);
                if (val < 0 || val > INT_MAX || *end)
                    invalid = 1;
                else
                    dp->udp_fake_count = val;
                break;

            case 'V':
                val = strtol(optarg, &end, 0);
                if (val <= 0 || val > USHRT_MAX)
                    invalid = 1;
                else {
                    dp->pf[0] = htons(val);
                    if (*end == '-') {
                        val = strtol(end + 1, &end, 0);
                        if (val <= 0 || val > USHRT_MAX)
                            invalid = 1;
                    }
                    if (*end)
                        invalid = 1;
                    else
                        dp->pf[1] = htons(val);
                }
                break;

            case 'R':
                val = strtol(optarg, &end, 0);
                if (val <= 0 || val > INT_MAX)
                    invalid = 1;
                else {
                    dp->rounds[0] = val;
                    if (*end == '-') {
                        val = strtol(end + 1, &end, 0);
                        if (val <= 0 || val > INT_MAX)
                            invalid = 1;
                    }
                    if (*end)
                        invalid = 1;
                    else
                        dp->rounds[1] = val;
                }
                break;

            case 'g':
                val = strtol(optarg, &end, 0);
                if (val <= 0 || val > 255 || *end)
                    invalid = 1;
                else {
                    params.def_ttl = val;
                    params.custom_ttl = 1;
                }
                break;

            case 'Y':
                dp->drop_sack = 1;
                break;

            case 'Z':
                params.wait_send = 1;
                break;

            case 'W':
                params.await_int = atoi(optarg);
                break;

            case 'C':
                if (get_addr(optarg, &dp->custom_dst_addr) < 0)
                    invalid = 1;
                else
                    dp->custom_dst = 1;
                break;

#ifdef __linux__
            case 'P':
                params.protect_path = optarg;
                break;
#endif
            case 0:
                break;

            case '?':
                reset_params();
                return -1;

            default:
                printf("?: %c\n", rez);
                reset_params();
                return -1;
        }
    }
    if (invalid) {
        fprintf(stderr, "invalid value: -%c %s\n", rez, optarg);
        reset_params();
        return -1;
    }
    if (all_limited) {
        dp = add_group(dp);
        if (!dp) {
            reset_params();
            return -1;
        }
    }
    if ((size_t )params.dp_n > sizeof(dp->bit) * 8) {
        LOG(LOG_E, "too many groups!\n");
    }
    if (params.baddr.sa.sa_family != AF_INET6) {
        params.ipv6 = 0;
    }
    if (!params.def_ttl) {
        if ((params.def_ttl = get_default_ttl()) < 1) {
            reset_params();
            return -1;
        }
    }
    params.mempool = mem_pool(MF_EXTRA, CMP_BYTES);
    if (!params.mempool) {
        uniperror("mem_pool");
        reset_params();
        return -1;
    }
    srand((unsigned int)time(0));
    return 0;
}
