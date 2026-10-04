package jusulsa.skill

import java.awt.event.KeyEvent

// a(1): 헬파이어,
// b(2): 공력증강,
// c(3): 마비,
// d(4): 활력,
// e(5): 혼돈,
// f(6): 절망,
// g(7): 저주,
// h(8): 삼매진화,
// i(9): 태양의기원,
// j(0): 지폭지술,
// 나머지는 shift+z + 알파벳
// m: 보호, n: 무장, o: 마기지체, q: 극진뢰격참주'첨, r: 진뢰격참주'첨, G: 중독
// A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z
const val SKILL_DELAY = 60L
const val HELLFIRE = KeyEvent.VK_1
const val GONGJEUNG = KeyEvent.VK_2
const val MABEE = KeyEvent.VK_3
const val HONDON = KeyEvent.VK_5
const val JULMANG = KeyEvent.VK_6
const val JEOJU = KeyEvent.VK_7
const val SAMME = KeyEvent.VK_8
const val HEAL = KeyEvent.VK_9
const val BOHO = KeyEvent.VK_M
const val MUJANG = KeyEvent.VK_N
const val MAGII = KeyEvent.VK_O
const val CHUM1 = KeyEvent.VK_Q
const val CHUM2 = KeyEvent.VK_R
const val JUNGDOK = KeyEvent.VK_G // 대문자 G
