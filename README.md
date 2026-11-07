# Zoltraak Moves

Mod bo sung cho **Zoltraak** (NeoForge 1.21.1): cam Zoltraak Staff de dung

| Phim | Chuc nang |
|------|-----------|
| **R** (doi duoc trong Controls) | Dash khi dung tren dat, cooldown 2.5s. **Khong gay sat thuong** |
| **Ctrl + Chuot phai** | **Meteor Storm**: vong ma thuat tai diem ngam (toi da 40 block), mua sao bang + thien thach lua trong 6s, moi vien AoE ban kinh 3 gay **10 atk**, cooldown 20s |
| **Space x2** | Nhan Space lan 2 khi o tren khong de Double Jump (1 lan moi lan roi dat) |

Can `zoltraak-1.0.0.jar` di kem. Cai tren **ca server va client**.

## Build bang GitHub (khong can cai gi tren may)
1. Tao repository moi tren GitHub, roi:
   ```
   git init
   git add .
   git commit -m "Zoltraak Moves"
   git branch -M main
   git remote add origin https://github.com/<ten-ban>/zoltraak-moves.git
   git push -u origin main
   ```
2. Vao tab **Actions** > workflow **build** > doi chay xong > tai artifact `zoltraak-moves-jar`.

## Build tren may
Can JDK 21 va Gradle 8.10+ (hoac chay `gradle wrapper` mot lan de tao `gradlew`):
```
gradle build
```
Jar nam o `build/libs/zoltraak_moves-1.0.0.jar`.

## Sat thuong
- Danh thuong bang **Zoltraak Staff**: **10 atk** (chinh `STAFF_ATTACK_DAMAGE` trong `ServerEvents.java`).
- Meteor Storm: 10 atk moi vien.
- Dash va Double Jump: khong gay sat thuong.

## Chinh thong so
Cac hang so o dau `MoveLogic.java`: `DASH_COOLDOWN_TICKS`, `DASH_SPEED`, `DASH_LIFT`, `JUMP_SPEED`; Meteor Storm chinh o dau `MeteorSkill.java` (`DAMAGE`, `COOLDOWN_TICKS`, `DURATION_TICKS`, `CIRCLE_RADIUS`, `IMPACT_RADIUS`).
