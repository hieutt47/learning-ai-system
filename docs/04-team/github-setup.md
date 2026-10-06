# GitHub và lần push đầu tiên

Repository công khai: https://github.com/hieutt47/learning-ai-system

Thư mục local đã có nhánh `main`, remote `origin` và được chuẩn bị commit khung. Chưa push nội dung từ máy lên GitHub; repo online còn trống đến khi bạn push.

## Bạn đẩy bộ khung lên

Mở PowerShell và chạy:

```powershell
Set-Location -LiteralPath 'C:\Users\Admin\OneDrive\Máy tính\Bài tập lớn Java\learning-ai-system'
git status
git push -u origin main
```

Nếu Git yêu cầu đăng nhập, dùng tài khoản GitHub của bạn qua Git Credential Manager. Không nhập mật khẩu GitHub vào mã nguồn hoặc remote URL. Nếu có thay đổi mới chưa commit, xem diff rồi add/commit phần mình chủ động sửa trước khi push.

## Mời thành viên sau khi push

Trên repo: Settings → Collaborators → Add people; mời bốn tài khoản thành viên, chờ họ nhận lời. Public cho phép xem/clone nhưng thành viên muốn push cần quyền collaborator. Chưa mời ai vì chưa có username và chưa chia việc.

Mỗi thành viên clone vào thư mục riêng:

```powershell
git clone https://github.com/hieutt47/learning-ai-system.git
cd learning-ai-system
git switch -c feat/<ma-task>-<chu-de>
```

Sau khi làm task: xem `git diff`, add các file liên quan, commit, push branch rồi mở Pull Request vào main. Reviewer khác owner; đổi API/schema có lead review. Quy trình chi tiết trong team-workflow.md.

## Thiết lập sau lần push đầu

- Bảo vệ main bằng ruleset/branch protection: PR trước merge và ít nhất một approval; nhóm thống nhất quyền lead trước cấu hình.
- Chưa bắt buộc CI check khi project chưa có code/CI.
- Email commit của repo local dùng địa chỉ GitHub noreply `208784897+hieutt47@users.noreply.github.com`; không đổi cấu hình Git toàn máy.
- Chưa tạo CI, license, nhãn issue, GitHub Project, mời thành viên hoặc push từ phiên này.

Lần sau đã có upstream: `git push`. Trước làm việc mới trên main, `git pull --ff-only` rồi tạo branch task. Không force push main.
