cd D:\Users\abhis\Documents\Projects_ChatGPT\Ongoing\distributed_system\kafka_server && gradle clean bootjar ^
&& cd D:\Users\abhis\Documents\Projects_ChatGPT\Ongoing\distributed_system\kafka_consumer && gradle clean bootjar ^
&& cd D:\Users\abhis\Documents\Projects_ChatGPT\Ongoing\distributed_system\Server1 && gradle clean bootjar ^
&& cd D:\Users\abhis\Documents\Projects_ChatGPT\Ongoing\distributed_system\servers && gradle clean bootjar ^
&& cd D:\Users\abhis\Documents\Projects_ChatGPT\Ongoing\distributed_system && docker-compose up --build -d