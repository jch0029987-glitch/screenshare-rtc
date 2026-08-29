#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <linux/input.h>

int main(int argc, char *argv[]) {
    if (argc < 2) return 1;

    int fd = open("/dev/input/event0", O_WRONLY | O_CLOEXEC);
    if (fd < 0) return 1;

    char *payload = argv[1];
    int x = 0, y = 0, action = 0;

    if (sscanf(payload, "%d,%d,%d", &action, &x, &y) >= 1) {
        struct input_event ev;

        if (x > 0 && y > 0) {
            memset(&ev, 0, sizeof(ev));
            ev.type = EV_ABS;
            ev.code = ABS_MT_POSITION_X;
            ev.value = x;
            write(fd, &ev, sizeof(ev));

            memset(&ev, 0, sizeof(ev));
            ev.type = EV_ABS;
            ev.code = ABS_MT_POSITION_Y;
            ev.value = y;
            write(fd, &ev, sizeof(ev));
        }

        memset(&ev, 0, sizeof(ev));
        ev.type = EV_KEY;
        ev.code = BTN_TOUCH;
        ev.value = (action == 0) ? 1 : 0;
        write(fd, &ev, sizeof(ev));

        memset(&ev, 0, sizeof(ev));
        ev.type = EV_SYN;
        ev.code = SYN_REPORT;
        ev.value = 0;
        write(fd, &ev, sizeof(ev));
    }

    close(fd);
    return 0;
}
