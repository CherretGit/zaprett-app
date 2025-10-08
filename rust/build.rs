use cmake::Config;

fn main() {
    let dst = Config::new(".").build_target("byedpi").build();
    let lib_path = dst.join("build");
    println!("cargo:rustc-link-search=native={}", lib_path.display());
    println!("cargo:rustc-link-lib=static=byedpi");
}
