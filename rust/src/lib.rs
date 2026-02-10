use android_logger::Config;
use core::ffi::{c_char, c_int};
use jni::JNIEnv;
use jni::objects::{JClass, JObjectArray, JString};
use jni::sys::jint;
use log::{LevelFilter, error, info};
use nix::sys::socket::{Shutdown, shutdown};
use std::ffi::CString;
use std::sync::atomic::{AtomicBool, Ordering};

static PROXY_RUNNING: AtomicBool = AtomicBool::new(false);

#[link(name = "byedpi", kind = "static")]
unsafe extern "C" {
    static mut server_fd: i32;
    static mut optind: i32;
    static mut optreset: i32;
    fn main(argc: c_int, argv: *const *const c_char) -> c_int;
    fn clear_params();
}

fn init_logger() {
    android_logger::init_once(Config::default().with_max_level(LevelFilter::Info));
}

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_cherret_zaprett_byedpi_NativeBridge_jniStartProxy(
    mut env: JNIEnv,
    _class: JClass,
    args: JObjectArray,
) -> jint {
    init_logger();
    if PROXY_RUNNING.swap(true, Ordering::SeqCst) {
        info!("proxy already running");
        return -1;
    }
    let argc = env.get_array_length(&args).unwrap_or(0) as usize;
    let mut cstrings: Vec<CString> = Vec::with_capacity(argc);
    for i in 0..argc {
        let jstr: JString = env
            .get_object_array_element(&args, i as i32)
            .unwrap()
            .into();
        let rust_str: String = env.get_string(&jstr).unwrap().into();
        cstrings.push(CString::new(rust_str).unwrap());
    }
    let mut argv: Vec<*const c_char> = cstrings.iter().map(|s| s.as_ptr()).collect();
    argv.push(std::ptr::null());
    info!("starting proxy");
    unsafe {
        optind = 1;
        optreset = 1;
        clear_params();
    }
    PROXY_RUNNING.store(true, Ordering::SeqCst);
    let ret = unsafe { main(argc as i32, argv.as_ptr()) };
    PROXY_RUNNING.store(false, Ordering::SeqCst);
    ret as jint
}

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_cherret_zaprett_byedpi_NativeBridge_jniStopProxy(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    init_logger();
    if !PROXY_RUNNING.load(Ordering::SeqCst) {
        info!("proxy already stopped");
        return -1;
    }
    info!("stopping proxy");
    let ret = unsafe { shutdown(server_fd, Shutdown::Both) };
    match ret {
        Ok(_) => {
            info!("proxy stopped successfully");
            0
        }
        Err(e) => {
            error!("failed to stop proxy {}", e);
            -1
        }
    }
}
