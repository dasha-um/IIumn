package main;
public interface Matrix
{
    Matrix mul(Matrix o);

    Matrix threadmul(Matrix o);

}