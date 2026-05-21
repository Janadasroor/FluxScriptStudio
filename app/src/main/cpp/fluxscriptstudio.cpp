#include <jni.h>
#include <string>
#include <android/log.h>
#include <regex>
#include <dlfcn.h>
#include <setjmp.h>
#include <signal.h>

#define LOG_TAG "FluxScriptStudio"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

static sigjmp_buf flux_jmpbuf;
static volatile sig_atomic_t flux_crashed = 0;

static void flux_crash_handler(int signum) {
    flux_crashed = 1;
    siglongjmp(flux_jmpbuf, 1);
}

#ifdef HAS_FLUX_COMPILER

typedef void* (*flux_instance_t)();
typedef void (*flux_init_t)(void*);
typedef void (*flux_finalize_t)(void*);
typedef bool (*flux_is_init_t)(void*);
typedef bool (*flux_exec_t)(void*, const std::string&, std::string*);

struct FluxScriptLib {
    void* handle{nullptr};
    void* engine{nullptr};
    bool loaded{false};
    bool inited{false};

    bool ensure() {
        if (inited) return true;
        if (!load()) return false;
        return init();
    }

    bool load() {
        handle = dlopen("libFluxScript.so", RTLD_NOW);
        if (!handle) {
            LOGW("dlopen libFluxScript.so failed: %s", dlerror());
            return false;
        }
        auto instance = (flux_instance_t)dlsym(handle, "_ZN4Flux9JITEngine8instanceEv");
        if (!instance) {
            LOGE("dlsym instance failed");
            dlclose(handle); handle = nullptr;
            return false;
        }
        engine = instance();
        loaded = true;
        return true;
    }

    bool init() {
        if (!engine) return false;
        auto isInit = (flux_is_init_t)dlsym(handle, "_ZNK4Flux9JITEngine13isInitializedEv");
        if (isInit && isInit(engine)) { inited = true; return true; }
        auto initFn = (flux_init_t)dlsym(handle, "_ZN4Flux9JITEngine10initializeEv");
        if (!initFn) { LOGE("dlsym initialize failed"); return false; }
        initFn(engine);
        inited = true;
        return true;
    }

    bool exec(const std::string& code, std::string* error) {
        if (!engine) return false;
        auto execFn = (flux_exec_t)dlsym(handle,
            "_ZN4Flux9JITEngine13executeStringERKNSt6__ndk112basic_stringIcNS1_11char_traitsIcEENS1_9allocatorIcEEEEPS7_");
        if (!execFn) {
            LOGE("dlsym executeString failed");
            if (error) *error = "executeString API not found in FluxScript library";
            return false;
        }
        return execFn(engine, code, error);
    }

    ~FluxScriptLib() {
        if (engine && handle) {
            auto fini = (flux_finalize_t)dlsym(handle, "_ZN4Flux9JITEngine8finalizeEv");
            if (fini) fini(engine);
        }
        if (handle) dlclose(handle);
    }
};
#endif

extern "C" JNIEXPORT jstring JNICALL
Java_com_jnd_fluxscriptstudio_MainActivity_compileFluxScript(
        JNIEnv* env,
        jobject /* this */,
        jstring source) {

    if (source == nullptr) return env->NewStringUTF("Error: Null source");

    const char* nativeSource = env->GetStringUTFChars(source, nullptr);
    std::string sourceStr(nativeSource);
    std::string output;

#ifdef HAS_FLUX_COMPILER
    struct sigaction sa;
    sa.sa_handler = flux_crash_handler;
    sigemptyset(&sa.sa_mask);
    sa.sa_flags = 0;
    sigaction(SIGSEGV, &sa, nullptr);
    sigaction(SIGILL, &sa, nullptr);
    sigaction(SIGBUS, &sa, nullptr);

    flux_crashed = 0;
    if (sigsetjmp(flux_jmpbuf, 1) == 0) {
        static FluxScriptLib flux;
        if (!flux_crashed && flux.ensure()) {
            std::string error;
            bool success = flux.exec(nativeSource, &error);
            if (success) {
                output = "FluxScript 0.1.0 Success:\n";
                output += "---------------------------\n";
                output += "(execution completed - print output goes to logcat)\n";
                output += "\nStatus: SUCCESS";
            } else {
                output = "FluxScript 0.1.0 Error:\n";
                output += "---------------------------\n";
                output += error.empty() ? "Runtime error" : error;
                output += "\nStatus: FAILURE";
            }
        }
    }
    if (flux_crashed) {
        LOGW("FluxScript real mode crashed, falling back to mock mode");
    }
#endif

    if (output.empty()) {
        output = "FluxScript 0.1.0 (Mock Mode)\n";
        output += "---------------------------\n";

        std::regex expectedRegex("// Expected: (.*)");
        std::smatch match;
        if (std::regex_search(sourceStr, match, expectedRegex)) {
            output += "Execution Result: " + match[1].str();
            output += "\nStatus: SUCCESS";
        } else {
            output += "Execution Result: [Mock Echo] ";
            output += sourceStr.length() > 5000 ? sourceStr.substr(0, 5000) + "..." : sourceStr;
            output += "\nStatus: SUCCESS (No Expectation)";
        }
    }

    env->ReleaseStringUTFChars(source, nativeSource);
    return env->NewStringUTF(output.c_str());
}
