package controller.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dto.AdminMemberDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.AdminService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/api/admin/members")
public class AdminMemberListController
        extends HttpServlet {

    private AdminService adminService;
    private Gson gson;

    @Override
    public void init() throws ServletException {

        adminService = new AdminService();

        gson = new GsonBuilder()
                .setDateFormat(
                        "yyyy-MM-dd HH:mm:ss"
                )
                .create();
    }


    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        setJsonResponse(response);

        try {

            List<AdminMemberDTO> memberList =
                    adminService.getMemberList();

            response.setStatus(
                    HttpServletResponse.SC_OK
            );

            gson.toJson(
                    Map.of(
                            "success", true,
                            "members", memberList
                    ),
                    response.getWriter()
            );

        } catch (RuntimeException e) {

            sendError(
                    response,
                    HttpServletResponse
                            .SC_INTERNAL_SERVER_ERROR,
                    "회원 목록을 불러오지 못했습니다."
            );
        }
    }


    private void setJsonResponse(
            HttpServletResponse response
    ) {

        response.setContentType(
                "application/json"
        );

        response.setCharacterEncoding(
                "UTF-8"
        );
    }


    private void sendError(
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {

        response.setStatus(status);

        gson.toJson(
                Map.of(
                        "success", false,
                        "message", message
                ),
                response.getWriter()
        );
    }
}