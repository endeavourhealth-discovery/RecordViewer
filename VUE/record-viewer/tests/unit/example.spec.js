import { describe, it, expect } from "vitest";
import { shallowMount } from "@vue/test-utils";
import LoginPage from "@/Login/LoginPage.vue";

describe("LoginPage.vue", () => {
  it("renders props.msg when passed", () => {
    const msg = "new message";
    const wrapper = shallowMount(LoginPage, {
      props: { msg },
    });
    expect(wrapper.text()).toContain(msg);
  });
});
