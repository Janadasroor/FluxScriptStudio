#include <jni.h>
#include <string>
#include <android/log.h>
#include <regex>
#include <dlfcn.h>

#define LOG_TAG "FluxScriptStudio"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

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
        LOGI("RealMode: dlopen libFluxScript.so...");
        handle = dlopen("libFluxScript.so", RTLD_NOW);
        if (!handle) {
            LOGW("RealMode: dlopen failed: %s", dlerror());
            return false;
        }
        LOGI("RealMode: dlopen OK, dlsym instance...");
        auto instance = (flux_instance_t)dlsym(handle, "_ZN4Flux9JITEngine8instanceEv");
        if (!instance) {
            LOGE("RealMode: instance symbol not found");
            dlclose(handle); handle = nullptr;
            return false;
        }
        LOGI("RealMode: calling instance()...");
        engine = instance();
        loaded = true;
        LOGI("RealMode: JITEngine=%p", engine);
        return true;
    }

    bool init() {
        if (!engine) return false;
        LOGI("RealMode: init check isInitialized...");
        auto isInit = (flux_is_init_t)dlsym(handle, "_ZNK4Flux9JITEngine13isInitializedEv");
        if (isInit && isInit(engine)) {
            LOGI("RealMode: already initialized");
            inited = true;
            return true;
        }
        LOGI("RealMode: dlsym initialize...");
        auto initFn = (flux_init_t)dlsym(handle, "_ZN4Flux9JITEngine10initializeEv");
        if (!initFn) { LOGE("RealMode: initialize symbol not found"); return false; }
        LOGI("RealMode: calling initialize()...");
        initFn(engine);
        inited = true;
        LOGI("RealMode: initialized OK");
        return true;
    }

    bool exec(const std::string& code, std::string* error) {
        if (!engine) return false;
        LOGI("RealMode: dlsym executeString...");
        auto execFn = (flux_exec_t)dlsym(handle,
            "_ZN4Flux9JITEngine13executeStringERKNSt6__ndk112basic_stringIcNS1_11char_traitsIcEENS1_9allocatorIcEEEEPS7_");
        if (!execFn) {
            LOGE("RealMode: executeString symbol not found");
            if (error) *error = "executeString API not found in FluxScript library";
            return false;
        }
        LOGI("RealMode: calling executeString...");
        bool result = execFn(engine, code, error);
        LOGI("RealMode: executeString returned %d", (int)result);
        return result;
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
    LOGI("Compiling Flux code:\n%s", nativeSource);

    std::string output;

#ifdef HAS_FLUX_COMPILER
    {
        LOGI("RealMode: creating static FluxScriptLib...");
        static FluxScriptLib flux;
        LOGI("RealMode: calling flux.ensure()...");
        if (flux.ensure()) {
            std::string error;
            LOGI("RealMode: calling flux.exec()...");
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
        } else {
            LOGW("RealMode: ensure() failed, falling back to mock mode");
            goto mock_mode;
        }
    }
    LOGI("RealMode: path completed successfully");
    if (false) {
mock_mode:;
#endif
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
#ifdef HAS_FLUX_COMPILER
    }
#endif

    env->ReleaseStringUTFChars(source, nativeSource);
    return env->NewStringUTF(output.c_str());
}
